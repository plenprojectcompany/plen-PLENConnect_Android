package jp.plen.scenography.utils;

import android.bluetooth.le.ScanFilter;
import android.os.ParcelUuid;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * PLEN の GATT Service の情報をまとめた定数クラス.
 */
public final class PlenGattConstants {
    private PlenGattConstants() {
    }

    public static final UUID PLEN_CONTROL_SERVICE_UUID = UUID.fromString("E1F40469-CFE1-43C1-838D-DDBC9DAFDDE6");
    public static final UUID RX_DATA_UUID = UUID.fromString("2ED17A59-FC21-488E-9204-503EB78158D7");
    public static final UUID TX_DATA_UUID = UUID.fromString("F90E9CFE-7E05-44A5-9D75-F13644D6F645");
    public static final List<ScanFilter> PLEN_FILTERS = Collections.singletonList(
            new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(PLEN_CONTROL_SERVICE_UUID))
                    .build());
}
