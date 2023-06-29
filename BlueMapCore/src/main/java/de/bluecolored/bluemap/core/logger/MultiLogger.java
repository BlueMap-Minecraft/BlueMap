package de.bluecolored.bluemap.core.logger;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class MultiLogger extends AbstractLogger {

    private final Logger[] logger;

    public MultiLogger(Logger... logger) {
        this.logger = logger;
    }

    @Override
    public void logError(String message, Throwable throwable) {
        for (int i = 0; i < logger.length; i++)
            logger[i].logError(message, throwable);
    }

    @Override
    public void logWarning(String message) {
        for (int i = 0; i < logger.length; i++)
            logger[i].logWarning(message);
    }

    @Override
    public void logInfo(String message) {
        for (int i = 0; i < logger.length; i++)
            logger[i].logInfo(message);
    }

    @Override
    public void logDebug(String message) {
        for (int i = 0; i < logger.length; i++)
            logger[i].logDebug(message);
    }

}
