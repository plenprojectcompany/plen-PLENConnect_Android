package jp.plen.plenconnect2.exceptions;

public class ScenographyException extends Exception {
    private static final long serialVersionUID = -6695500617926166432L;

    public ScenographyException() {
    }

    public ScenographyException(String detailMessage) {
        super(detailMessage);
    }

    public ScenographyException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public ScenographyException(Throwable throwable) {
        super(throwable);
    }
}
