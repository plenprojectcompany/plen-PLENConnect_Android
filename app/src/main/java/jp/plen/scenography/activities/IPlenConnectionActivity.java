package jp.plen.scenography.activities;

import android.support.annotation.NonNull;

import java.util.List;

public interface IPlenConnectionActivity {

    void notifyBluetoothUnavailable();

    void notifyPlenScanning();

    void notifyPlenScanCancel();

    void notifyPlenScanComplete(@NonNull List<String> addresses);

    void notifyPlenConnectionChanged(boolean connected, boolean now);

    void notifyWriteTxDataCompleted();

    void notifyConnectionError(@NonNull Throwable e);
}
