package jp.plen.bluetooth.le;

/**
 * PLENとのBluetooth通信関連の例外.
 * Created by kzm4269 on 15/07/20.
 */
public class PlenBluetoothLeException extends Exception {
    private static final String TAG = PlenBluetoothLeException.class.getSimpleName();

    public PlenBluetoothLeException() {
    }

    public PlenBluetoothLeException(String detailMessage) {
        super(detailMessage);
    }

    public PlenBluetoothLeException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public PlenBluetoothLeException(Throwable throwable) {
        super(throwable);
    }
}
