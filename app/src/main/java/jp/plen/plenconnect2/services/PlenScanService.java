package jp.plen.plenconnect2.services;

import android.app.Service;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;

import org.androidannotations.annotations.EService;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import jp.plen.rx.binding.Property;
import jp.plen.plenconnect2.R;
import jp.plen.plenconnect2.exceptions.PlenConnectionException;
import jp.plen.plenconnect2.exceptions.ScenographyException;
import jp.plen.plenconnect2.utils.BluetoothUtil;
import jp.plen.plenconnect2.utils.PlenGattConstants;
import jp.plen.plenconnect2.utils.WatchDogTimer;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

@EService
public class PlenScanService extends Service {
    private static final String TAG = PlenScanService.class.getSimpleName();
    private final IBinder mBinder = new Binder();
    private final WatchDogTimer mWatchDogTimer = new WatchDogTimer();  // 遅延終了用タイマー
    private final Property<State> mState = Property.create(State.STOP);
    private final PublishSubject<Request> mRequest = PublishSubject.create();
    private final List<ScanResult> mScanResults = new LinkedList<>();
    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, @NonNull ScanResult result) {
            mScanResults.add(result);
            post(new ScanResultsUpdateEvent(result));
        }

        @Override
        public void onBatchScanResults(@NonNull List<ScanResult> results) {
            for (ScanResult result : results) {
                mScanResults.add(result);
                post(new ScanResultsUpdateEvent(result));
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            String message = "onScanFailed: code=" + errorCode;
            postErrorEvent(new PlenConnectionException(message));
        }
    };
    private final CompositeSubscription mSubscriptions = new CompositeSubscription();
    @NonNull private StateNotification mLastStateNotification = new StateNotification(State.STOP, mScanResults);

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate ");
        EventBus.getDefault().register(this);

        // 遅延終了用タイマーの初期設定
        int stop_delay = getBaseContext().getResources().getInteger(R.integer.plen_scan_service_stop_delay_sec);
        mWatchDogTimer.init(this::stopSelf, stop_delay, TimeUnit.SECONDS);

        mSubscriptions.clear();
        // 状態遷移時にイベントを発行するように設定
        mSubscriptions.clear();
        mSubscriptions.add(Observable
                .zip(mState.asObservable(), mState.asObservable().skip(1), StateTransitionEvent::new)
                .doOnNext(ev -> {
                    mLastStateNotification = new StateNotification(ev.getNewState(), mScanResults);
                    postStateNotification();
                })
                .doOnNext(ev -> Log.i(TAG, ev.getNewState().getDescription(getApplication())))
                .subscribe(this::post));
        // リクエストを処理するオブザーバを設定
        mSubscriptions.add(
                mRequest.observeOn(Schedulers.newThread())
                        .onBackpressureBuffer()
                        .subscribe(this::process));

        post(new StateNotification(State.STOP, mScanResults));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;  // START_NOT_STICKY: 強制終了時に再起動しない
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy ");
        mWatchDogTimer.stop();
        stopScan(new StopScanRequest());
        mSubscriptions.unsubscribe();
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mWatchDogTimer.stop();
        startService(new Intent(getBaseContext(), PlenScanService_.class));  // Unbindされても終了しないように
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mWatchDogTimer.restart();
        return true;
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

    private void startScan(@NonNull StartScanRequest request) {
        if (getState() == State.SCANNING) {
            return;
        }

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

        mScanResults.clear();
        try {
            BluetoothUtil.getBluetoothAdapter(getApplication())
                    .getBluetoothLeScanner()
                    .startScan(PlenGattConstants.PLEN_FILTERS, settings, mScanCallback);
        } catch (ScenographyException e) {
            postErrorEvent(e);
            return;
        }

        Observable<Object> stopEvent = Observable.amb(
                Observable.timer(request.getTimeout(), request.getUnit())
                        .doOnNext(t -> stopScan(new StopScanRequest())),
                mState.asObservable()
                        .skipWhile(state -> state != State.SCANNING)
                        .skipWhile(state -> state != State.STOP));
        mSubscriptions.add(stopEvent.first().subscribe());

        mState.set(State.SCANNING);
    }

    private void stopScan(StopScanRequest request) {
        if (getState() != State.SCANNING) {
            return;
        }

        try {
            BluetoothUtil.getBluetoothAdapter(getApplication())
                    .getBluetoothLeScanner()
                    .stopScan(mScanCallback);
        } catch (ScenographyException e) {
            postErrorEvent(e);
        }
        mState.set(State.STOP);
    }

    private void post(Event e) {
        EventBus.getDefault().post(e);
    }

    private void postErrorEvent(@NonNull Throwable source) {
        Log.w(TAG, source.toString());
        post(new ErrorEvent(source));
    }

    private void postStateNotification() {
        post(new StateNotification(getState(), mScanResults));
    }

    private State getState() {
        return mState.get().get();
    }

    public enum State {
        STOP(R.string.log_ble_scan_complete),
        SCANNING(R.string.log_ble_scanning);

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
        protected abstract void process(PlenScanService service);
    }

    public static class StartScanRequest extends Request {
        private final long mTimeout;
        @NonNull private final TimeUnit mUnit;

        public StartScanRequest(long timeout, @NonNull TimeUnit unit) {
            mTimeout = timeout;
            mUnit = unit;
        }

        public long getTimeout() {
            return mTimeout;
        }

        @NonNull
        public TimeUnit getUnit() {
            return mUnit;
        }

        @Override
        protected void process(@NonNull PlenScanService service) {
            service.startScan(this);
        }
    }

    public static class StopScanRequest extends Request {
        @Override
        protected void process(@NonNull PlenScanService service) {
            service.stopScan(this);
        }
    }

    public static class StateNotificationRequest extends Request {
        @Override
        protected void process(@NonNull PlenScanService service) {
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

    public class StateTransitionEvent extends Event {
        @NonNull private State oldState;
        @NonNull private State newState;

        public StateTransitionEvent(@NonNull State oldState, @NonNull State newState) {
            this.oldState = oldState;
            this.newState = newState;
        }

        @NonNull
        public State getOldState() {
            return oldState;
        }

        @NonNull
        public State getNewState() {
            return newState;
        }
    }

    public class StateNotification extends Event {
        @NonNull private final State mState;
        @NonNull private final List<ScanResult> mScanResults;

        public StateNotification(@NonNull State state, @NonNull List<ScanResult> scanResults) {
            mState = state;
            mScanResults = scanResults;
        }

        @NonNull
        public State getState() {
            return mState;
        }

        @NonNull
        public List<ScanResult> getScanResults() {
            return mScanResults;
        }
    }

    public class ScanResultsUpdateEvent extends Event {
        @NonNull private final ScanResult mScanResult;

        public ScanResultsUpdateEvent(@NonNull ScanResult result) {
            mScanResult = result;
        }

        @NonNull
        public ScanResult getNewResult() {
            return mScanResult;
        }
    }
}
