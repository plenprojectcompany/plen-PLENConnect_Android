package jp.plen.plenconnect2.activities;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.eccyan.optional.Optional;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import jp.plen.plenconnect2.R;
import jp.plen.plenconnect2.exceptions.BluetoothUnavailableException;
import jp.plen.plenconnect2.exceptions.LocationUnavailableException;
import jp.plen.plenconnect2.models.preferences.MainPreferences_;
import jp.plen.plenconnect2.services.PlenConnectionService;
import jp.plen.plenconnect2.services.PlenScanService;
import rx.Observable;

@EBean
public class PlenConnectionActivityPresenter {
    private static final String TAG = PlenConnectionActivityPresenter.class.getSimpleName();
    @RootContext Context mContext;
    @NonNull private Optional<IPlenConnectionActivity> mView = Optional.empty();
    @NonNull private Optional<List<ScanResult>> mScanResults = Optional.empty();
    @Pref MainPreferences_ mPref;
    private boolean mConnected;
    private boolean mWriting;

    public void bind(@NonNull IPlenConnectionActivity view) {
        Log.d(TAG, "bind ");
        mView = Optional.of(view);
        EventBus.getDefault().register(this);
        EventBus.getDefault().post(new PlenScanService.StateNotificationRequest());
        EventBus.getDefault().post(new PlenConnectionService.StateNotificationRequest());
    }

    public void unbind() {
        Log.d(TAG, "onDestroy ");
        EventBus.getDefault().unregister(this);
        mView = Optional.empty();
    }

    public void startScan() {
        mScanResults = Optional.of(new ArrayList<>());

        int timeout = mContext.getResources().getInteger(R.integer.plen_scan_timeout_sec);
        EventBus.getDefault().post(new PlenScanService.StartScanRequest(timeout, TimeUnit.SECONDS));
    }

    public void cancelScan() {
        mScanResults = Optional.empty();

        EventBus.getDefault().post(new PlenScanService.StopScanRequest());
    }

    public void connectPlen(@NonNull String address) {
        EventBus.getDefault().post(new PlenConnectionService.ConnectRequest(address));
    }

    public void disconnectPlen() {
        EventBus.getDefault().post(new PlenConnectionService.DisconnectRequest());
    }

    public boolean getPlenConnected() {
        return mConnected;
    }

    public boolean getWriting() {
        return mWriting;
    }

    public void onEvent(@NonNull PlenScanService.ErrorEvent event) {
        Throwable source = event.getSource();
        mView.ifPresent(view -> {
            if (source instanceof BluetoothUnavailableException) {
                view.notifyBluetoothUnavailable();
            } else if (source instanceof LocationUnavailableException) {
                view.notifyLocationUnavailable();
            } else {
                view.notifyConnectionError(source);
            }
        });
    }

    public void onEvent(@NonNull PlenScanService.ScanResultsUpdateEvent event) {
        mScanResults.ifPresent(r -> r.add(event.getNewResult()));
    }

    public void onEvent(@NonNull PlenScanService.StateNotification notification) {
        mScanResults = Optional.of(notification.getScanResults());
        mView.ifPresent(fragment -> {
            switch (notification.getState()) {
                case SCANNING:
                    fragment.notifyPlenScanning();
                    break;
            }
        });
    }

    public void onEvent(@NonNull PlenScanService.StateTransitionEvent event) {
        mView.ifPresent(fragment -> {
            switch (event.getNewState()) {
                case SCANNING:
                    fragment.notifyPlenScanning();
                    break;
                case STOP:
                    if (!mScanResults.isPresent()) {
                        fragment.notifyPlenScanCancel();
                    } else {
                        List<String> addresses = Observable.from(mScanResults.get())
                                .map(ScanResult::getDevice)
                                .map(BluetoothDevice::getAddress)
                                .distinct().toList().toBlocking().single();
                        fragment.notifyPlenScanComplete(addresses);
                    }
                    break;
            }
        });
    }

    public void onEvent(@NonNull PlenConnectionService.StateNotification notification) {
        mView.ifPresent(fragment -> {
            mWriting = notification.getState() == PlenConnectionService.State.WRITING;
            switch (notification.getState()) {
                case DISCONNECTED:
                    fragment.notifyPlenConnectionChanged(false, false);
                    mConnected = false;
                    break;
                case SERVICE_IDLE:
                    fragment.notifyPlenConnectionChanged(true, false);
                    mConnected = true;
                    break;
                case INITIALIZED:
                    Optional.ofNullable(mPref.defaultPlenAddress().get())
                            .filter(address -> !address.isEmpty())
                            .ifPresent(this::connectPlen);
                    break;
            }
        });
    }

    public void onEvent(@NonNull PlenConnectionService.StateTransitionEvent event) {
        mView.ifPresent(fragment -> {
            switch (event.getNewState()) {
                case DISCONNECTED:
                    fragment.notifyPlenConnectionChanged(false, true);
                    break;
                case SERVICE_DISCOVERED:
                    fragment.notifyPlenConnectionChanged(true, true);
                    break;
                case SERVICE_IDLE:
                    if (event.getOldState() == PlenConnectionService.State.WRITING) {
                        fragment.notifyWriteTxDataCompleted();
                    }
                    break;
            }
        });
    }
}
