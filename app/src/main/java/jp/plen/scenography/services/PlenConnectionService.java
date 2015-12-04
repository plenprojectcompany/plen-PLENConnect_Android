package jp.plen.scenography.services;

import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;

import com.eccyan.optional.Optional;

import org.androidannotations.annotations.EService;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import jp.plen.rx.binding.Property;
import jp.plen.scenography.R;
import jp.plen.scenography.exceptions.PlenConnectionException;
import jp.plen.scenography.exceptions.ScenographyException;
import jp.plen.scenography.utils.BluetoothUtil;
import jp.plen.scenography.utils.PlenGattConstants;
import jp.plen.scenography.utils.WatchDogTimer;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

@EService
public class PlenConnectionService extends Service {
    private static final String TAG = PlenConnectionService.class.getSimpleName();
    private static final int MAX_PACKET_SIZE = 20;
    private final IBinder mBinder = new Binder();
    private final WatchDogTimer mWatchDogTimer = new WatchDogTimer();
    private final Property<State> mState = Property.create(State.INITIALIZED);
    private final PublishSubject<Request> mRequest = PublishSubject.create();
    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback();
    private final CompositeSubscription mSubscriptions = new CompositeSubscription();
    private final Queue<String> mTxDataQueue = new LinkedBlockingQueue<>();
    @NonNull private Optional<BluetoothGatt> mGatt = Optional.empty();
    @NonNull private Optional<BluetoothGattCharacteristic> mTxCharacteristic = Optional.empty();
    @NonNull private StateNotification mLastStateNotification = new StateNotification(State.INITIALIZED);

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate ");
        EventBus.getDefault().register(this);


        int stop_delay = getBaseContext().getResources().getInteger(R.integer.plen_connection_service_stop_delay_sec);
        mWatchDogTimer.init(this::stopSelf, stop_delay, TimeUnit.SECONDS);

        mSubscriptions.clear();
        mSubscriptions.add(Observable
                .zip(mState.asObservable(), mState.asObservable().skip(1), StateTransitionEvent::new)
                .doOnNext(ev -> {
                    mLastStateNotification = new StateNotification(ev.getNewState());
                    postStateNotification();
                })
                .doOnNext(ev -> Log.i(TAG, ev.getNewState().getDescription(getApplication())))
                .subscribe(this::post));
        mSubscriptions.add(
                mRequest.observeOn(Schedulers.newThread())
                        .onBackpressureBuffer()
                        .subscribe(this::process));

        postStateNotification();
        mState.set(State.DISCONNECTED);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy ");
        disconnect(new DisconnectRequest());
        mSubscriptions.unsubscribe();
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mWatchDogTimer.restart();
        return true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mWatchDogTimer.stop();
        startService(new Intent(getBaseContext(), PlenConnectionService_.class));
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        mWatchDogTimer.stop();
        super.onRebind(intent);
    }

    public void onEvent(Request request) {
        mRequest.onNext(request);
    }

    synchronized private void process(@NonNull Request request) {
        request.process(this);
    }

    private void connect(@NonNull ConnectRequest request) {
        if (getState() != State.DISCONNECTED) {
            mRequest.onNext(new DisconnectRequest());
            mRequest.onNext(request);
            return;
        }

        mState.set(State.CONNECTING);
        try {
            mGatt = Optional.ofNullable(BluetoothUtil.getBluetoothAdapter(getApplication())
                    .getRemoteDevice(request.mAddress)
                    .connectGatt(getApplication(), false, mBluetoothGattCallback));
        } catch (ScenographyException e) {
            postErrorEvent(e);
            mState.set(State.DISCONNECTED);
            return;
        }
        if (!mGatt.isPresent()) {
            resetGatt(getString(R.string.log_ble_connecting_failed));
        }
    }

    private void disconnect(@NonNull DisconnectRequest request) {
        mGatt.ifPresent(gatt -> {
            if (getState() != State.DISCONNECTED) {
                mState.set(State.DISCONNECTING);
                gatt.disconnect();
            }
        });

        mState.asObservable()
                .filter(s -> s == State.DISCONNECTED)
                .first()
                .observeOn(Schedulers.immediate())
                .subscribe();
    }

    private void write(@NonNull WriteRequest request) {
        String data = request.getData();
        // MAX_PACKET_SIZE バイトごとに分割してバッファに貯める
        for (int i = 0; i < data.length(); i += MAX_PACKET_SIZE) {
            mTxDataQueue.offer(data.substring(i, Math.min(i + MAX_PACKET_SIZE, data.length())));
        }
        if (getState() == State.SERVICE_IDLE) {
            writeNextTxData();
        }
    }

    private void writeNextTxData() {
        if (mTxDataQueue.isEmpty()) {
            if (getState() == State.WRITING) {
                mState.set(State.SERVICE_IDLE);
            }
            return;
        }

        mGatt.ifPresent(gatt -> mTxCharacteristic.ifPresent(tx -> {
            tx.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            tx.setValue(mTxDataQueue.poll());
            boolean isWriting = gatt.writeCharacteristic(tx);
            if (!isWriting) {
                postErrorEvent(new PlenConnectionException(
                        getString(R.string.log_ble_writing_failed)));
                mState.set(State.DISCONNECTED);
                mState.set(State.CONNECTING);
                if (!gatt.connect() || !gatt.discoverServices()) {
                    resetGatt(getString(R.string.log_ble_connecting_failed));
                }
                return;
            }
            mState.set(State.WRITING);
        }));
    }

    private void resetGatt(@NonNull String errorLog) {
        postErrorEvent(new PlenConnectionException(errorLog));
        mGatt.ifPresent(BluetoothGatt::close);
        mGatt = Optional.empty();
        mState.set(State.DISCONNECTED);
    }

    private void post(Event e) {
        EventBus.getDefault().post(e);
    }

    private void postErrorEvent(@NonNull Throwable source) {
        Log.w(TAG, source.toString());
        post(new ErrorEvent(source));
    }

    private void postStateNotification() {
        post(mLastStateNotification);
    }

    private State getState() {
        return mState.get().get();
    }

    public enum State {
        INITIALIZED(R.string.log_plen_connection_service_initialized),
        DISCONNECTED(R.string.log_ble_disconnected),
        CONNECTING(R.string.log_ble_connecting),
        CONNECTED(R.string.log_ble_connected),
        SERVICE_DISCOVERING(R.string.log_ble_service_discovering),
        SERVICE_DISCOVERED(R.string.log_ble_service_discovered),
        SERVICE_IDLE(R.string.log_ble_service_idle),
        WRITING(R.string.log_ble_writing),
        DISCONNECTING(R.string.log_ble_disconnecting),;

        @StringRes
        private int mDescriptionRes;

        State(@StringRes int descriptionRes) {
            mDescriptionRes = descriptionRes;
        }

        @NonNull
        public String getDescription(@NonNull Context context) {
            return context.getString(mDescriptionRes);
        }
    }

    public static abstract class Request {
        protected abstract void process(PlenConnectionService service);
    }

    public static class ConnectRequest extends Request {
        @NonNull private final String mAddress;

        public ConnectRequest(@NonNull String address) {
            mAddress = address;
        }

        protected void process(@NonNull PlenConnectionService service) {
            service.connect(this);
        }
    }

    public static class DisconnectRequest extends Request {
        protected void process(@NonNull PlenConnectionService service) {
            service.disconnect(this);
        }
    }

    public static class WriteRequest extends Request {
        @NonNull private final String mData;

        public WriteRequest(@NonNull String data) {
            mData = data;
        }

        protected void process(@NonNull PlenConnectionService service) {
            service.write(this);
        }

        @NonNull
        public String getData() {
            return mData;
        }
    }

    public static class StateNotificationRequest extends Request {
        protected void process(@NonNull PlenConnectionService service) {
            service.postStateNotification();
        }
    }

    public abstract class Event {
    }

    public class ErrorEvent extends Event {
        @NonNull private final Throwable mSource;

        public ErrorEvent(@NonNull Throwable source) {
            mSource = source;
        }

        @NonNull
        public Throwable getSource() {
            return mSource;
        }
    }

    public class StateNotification extends Event {
        @NonNull private final State mState;

        public StateNotification(@NonNull State state) {
            mState = state;
        }

        @NonNull
        public State getState() {
            return mState;
        }
    }

    public class StateTransitionEvent extends Event {
        @NonNull private final State mNewState;
        @NonNull private final State mOldState;

        public StateTransitionEvent(@NonNull State oldState, @NonNull State newState) {
            mNewState = newState;
            mOldState = oldState;
        }

        @NonNull
        public State getNewState() {
            return mNewState;
        }

        @NonNull
        public State getOldState() {
            return mOldState;
        }
    }

    private class BluetoothGattCallback extends android.bluetooth.BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(@NonNull BluetoothGatt gatt, int status, int newState) {
            mGatt = Optional.of(gatt);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                resetGatt(getString(R.string.log_ble_connecting_or_disconnecting_failed));
                return;
            }

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    mState.set(State.CONNECTED);
                    boolean discoveryStarted = gatt.discoverServices();
                    if (discoveryStarted) {
                        mState.set(State.SERVICE_DISCOVERING);
                    } else {
                        resetGatt(getString(R.string.log_ble_services_discovering_failed));
                    }
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    gatt.close();
                    mGatt = Optional.empty();
                    mState.set(State.DISCONNECTED);
                    break;

                default:
                    resetGatt("onConnectionStateChange() called with unknown newState: " + newState);
            }
        }

        @Override
        public void onServicesDiscovered(@NonNull BluetoothGatt gatt, int status) {
            mGatt = Optional.of(gatt);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                resetGatt(getString(R.string.log_ble_discovering_failed));
                return;
            }

            Observable.from(gatt.getServices())
                    .map(BluetoothGattService::getUuid)
                    .toSortedList()
                    .flatMap(Observable::from)
                    .toBlocking()
                    .forEach(uuid -> Log.i(TAG, getString(R.string.log_ble_found_service, uuid)));

            BluetoothGattService plenControlService =
                    gatt.getService(PlenGattConstants.PLEN_CONTROL_SERVICE_UUID);
            if (plenControlService == null) {
                resetGatt(getString(R.string.log_ble_control_service_not_found));
                return;
            }

            mTxCharacteristic = Optional.ofNullable(
                    plenControlService.getCharacteristic(PlenGattConstants.TX_DATA_UUID));
            if (!mTxCharacteristic.isPresent()) {
                resetGatt(getString(R.string.log_ble_txdata_not_found));
                return;
            }
            mTxDataQueue.clear();

            mState.set(State.SERVICE_DISCOVERED);
            mState.set(State.SERVICE_IDLE);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            mGatt = Optional.of(gatt);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                postErrorEvent(new PlenConnectionException(
                        getString(R.string.log_ble_writing_failed)));
                return;
            }
            writeNextTxData();
        }
    }
}
