package de.bluecolored.bluemap.core.storage.sql;

public class SQLDriverException extends Exception {

    public SQLDriverException() {
        super();
    }

    public SQLDriverException(String message) {
        super(message);
    }

    public SQLDriverException(Throwable cause) {
        super(cause);
    }

    public SQLDriverException(String message, Throwable cause) {
        super(message, cause);
    }

}
