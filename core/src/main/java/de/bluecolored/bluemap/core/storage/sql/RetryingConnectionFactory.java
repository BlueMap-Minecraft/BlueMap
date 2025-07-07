package de.bluecolored.bluemap.core.storage.sql;

import de.bluecolored.bluemap.core.logger.Logger;
import org.apache.commons.dbcp2.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class RetryingConnectionFactory implements ConnectionFactory {

    private final ConnectionFactory delegate;
    private final int maxAttempts;
    private final long initialDelayMs;
    private final long maxDelayMs;
    private final double backoffMultiplier;

    public RetryingConnectionFactory(ConnectionFactory delegate, int maxAttempts, long initialDelayMs, long maxDelayMs, double backoffMultiplier) {
        this.delegate = delegate;
        this.maxAttempts = maxAttempts;
        this.initialDelayMs = initialDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.backoffMultiplier = backoffMultiplier;
    }

    @Override
    public Connection createConnection() throws SQLException {
        long delay = initialDelayMs;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Logger.global.logDebug("Creating new SQL-Connection...");
                return delegate.createConnection();
            } catch (SQLException ex) {
                if (attempt == maxAttempts) {
                    throw ex;
                }

                Logger.global.logWarning(String.format(
                        "Exception caught while attempting to create an SQL connection. Waiting for %.1f second(s) before retrying.",
                        delay / 1000.0
                ));

                safeSleep(delay);
                delay = Math.min((long) (delay * backoffMultiplier), maxDelayMs);
            }
        }

        throw new IllegalStateException();
    }

    private void safeSleep(long delay) throws SQLException {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new SQLException("Interrupted while waiting to retry connection.", e);
        }
    }
}
