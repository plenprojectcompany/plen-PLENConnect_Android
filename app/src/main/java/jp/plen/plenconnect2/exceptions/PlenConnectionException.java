package jp.plen.plenconnect2.exceptions;

/**
 * PLENとのBluetooth通信関連の例外.
 * Created by kzm4269 on 15/07/20.
 */
public class PlenConnectionException extends ScenographyException {
    private static final long serialVersionUID = 1284089615806286010L;

    public PlenConnectionException() {
    }

    public PlenConnectionException(String detailMessage) {
        super(detailMessage);
    }

    public PlenConnectionException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public PlenConnectionException(Throwable throwable) {
        super(throwable);
    }
}
