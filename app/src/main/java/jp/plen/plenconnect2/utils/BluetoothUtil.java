package jp.plen.plenconnect2.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.location.LocationManager;
import android.support.annotation.NonNull;

import com.eccyan.optional.Optional;

import jp.plen.plenconnect2.exceptions.BluetoothUnavailableException;
import jp.plen.plenconnect2.exceptions.LocationUnavailableException;
import jp.plen.plenconnect2.exceptions.PlenConnectionException;

public final class BluetoothUtil {
    private static final String TAG = BluetoothUtil.class.getSimpleName();

    private BluetoothUtil() {
    }

    @NonNull
    public static BluetoothAdapter getBluetoothAdapter(@NonNull Context context) throws PlenConnectionException, LocationUnavailableException {
        Optional.ofNullable((LocationManager) context.getSystemService(Context.LOCATION_SERVICE))
                .filter(lm -> lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                .orElseThrow(LocationUnavailableException::new);
        return Optional.ofNullable((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE))
                .map(BluetoothManager::getAdapter)
                .filter(BluetoothAdapter::isEnabled)
                .orElseThrow(BluetoothUnavailableException::new);
    }
}
