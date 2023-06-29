package de.bluecolored.bluemap.core.logger;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaLogger extends AbstractLogger {

    private final Logger out;

    public JavaLogger(Logger out) {
        this.out = out;
    }

    @Override
    public void logError(String message, Throwable throwable) {
        out.log(Level.SEVERE, message, throwable);
    }

    @Override
    public void logWarning(String message) {
        out.log(Level.WARNING, message);
    }

    @Override
    public void logInfo(String message) {
        out.log(Level.INFO, message);
    }

    @Override
    public void logDebug(String message) {
        if (out.isLoggable(Level.FINE)) out.log(Level.FINE, message);
    }

    @Override
    public void noFloodDebug(String message) {
        if (out.isLoggable(Level.FINE)) super.noFloodDebug(message);
    }

    @Override
    public void noFloodDebug(String key, String message) {
        if (out.isLoggable(Level.FINE)) super.noFloodDebug(key, message);
    }

}
