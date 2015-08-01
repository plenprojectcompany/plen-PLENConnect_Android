package jp.plen.bluetooth.le;

import java.util.UUID;

/**
 * PLENのGATT Serviceの情報をまとめた定数クラス.
 * Created by kzm4269 on 15/07/20.
 */
public final class PlenGattConstants {
    private PlenGattConstants(){}

    public static final String GAP_DEVICE_NAME = "PLEND";
    public static final UUID PLEN_CONTROL_SERVICE_UUID = UUID.fromString("E1F40469-CFE1-43C1-838D-DDBC9DAFDDE6");
    public static final UUID RX_DATA_UUID = UUID.fromString("2ED17A59-FC21-488E-9204-503EB78158D7");
    public static final UUID TX_DATA_UUID = UUID.fromString("F90E9CFE-7E05-44A5-9D75-F13644D6F645");
}
