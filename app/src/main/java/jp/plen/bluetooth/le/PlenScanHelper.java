package jp.plen.bluetooth.le;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;

/**
 * 接続可能なPLENを探すためのHelperクラス
 * Created by kzm4269 on 15/07/20.
 */
public class PlenScanHelper {
    private static final String TAG = PlenScanHelper.class.getSimpleName();
    public static final ScanFilter PLEN_FILTER = new ScanFilter.Builder()
            .setDeviceName(PlenGattConstants.GAP_DEVICE_NAME)
            .setServiceUuid(new ParcelUuid(PlenGattConstants.PLEN_CONTROL_SERVICE_UUID))
            .build();

    public static Observable<ScanResult> scan(@NonNull final BluetoothAdapter bluetoothAdapter, final int timeout) {
        return Observable.create(new Observable.OnSubscribe<ScanResult>() {
            @Override
            public void call(final Subscriber<? super ScanResult> subscriber) {
                final BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                List<ScanFilter> scanFilters = new ArrayList<>();
                scanFilters.add(PLEN_FILTER);

                ScanSettings scanSettings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                        .build();

                ScanCallback scanCallback = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        subscriber.onNext(result);
                    }

                    @Override
                    public void onBatchScanResults(List<ScanResult> results) {
                        for (ScanResult result : results)
                            subscriber.onNext(result);
                    }

                    @Override
                    public void onScanFailed(int errorCode) {
                        String message = "onScanFailed: errorCode=" + errorCode;
                        Log.w(TAG, message);
                        subscriber.onError(new PlenBluetoothLeException(message));
                        bluetoothLeScanner.stopScan(null);
                    }
                };

                bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback);
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException ignored) {
                }
                bluetoothLeScanner.stopScan(scanCallback);
                subscriber.onCompleted();
            }
        });
    }
}
