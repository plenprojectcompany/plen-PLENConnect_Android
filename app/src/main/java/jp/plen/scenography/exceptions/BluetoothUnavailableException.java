package jp.plen.scenography.exceptions;

public class BluetoothUnavailableException extends PlenConnectionException {
    private static final long serialVersionUID = 977731427742912901L;

    public BluetoothUnavailableException() {
    }

    public BluetoothUnavailableException(String detailMessage) {
        super(detailMessage);
    }

    public BluetoothUnavailableException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public BluetoothUnavailableException(Throwable throwable) {
        super(throwable);
    }
}
