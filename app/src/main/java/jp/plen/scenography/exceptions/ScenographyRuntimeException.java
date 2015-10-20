package jp.plen.scenography.exceptions;

public class ScenographyRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 880439356099888359L;

    public ScenographyRuntimeException() {
    }

    public ScenographyRuntimeException(String detailMessage) {
        super(detailMessage);
    }

    public ScenographyRuntimeException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public ScenographyRuntimeException(Throwable throwable) {
        super(throwable);
    }
}
