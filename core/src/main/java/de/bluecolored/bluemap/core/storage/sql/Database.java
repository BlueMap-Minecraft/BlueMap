/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.core.storage.sql;

import de.bluecolored.bluemap.core.logger.Logger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.dbcp2.*;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

@Getter
@RequiredArgsConstructor
public class Database implements Closeable {

    private final DataSource dataSource;
    private boolean isClosed = false;

    public Database(String url, Map<String, String> properties, int maxPoolSize) {
        Properties props = new Properties();
        props.putAll(properties);

        this.dataSource = createDataSource(new DriverManagerConnectionFactory(url, props), maxPoolSize);
    }

    public Database(String url, Map<String, String> properties, int maxPoolSize, Driver driver) {
        Properties props = new Properties();
        props.putAll(properties);

        ConnectionFactory connectionFactory = new DriverConnectionFactory(
                driver,
                url,
                props
        );

        this.dataSource = createDataSource(connectionFactory, maxPoolSize);
    }

    public void run(ConnectionConsumer action) throws IOException {
        run((ConnectionFunction<Void>) action);
    }

    public <R> R run(ConnectionFunction<R> action) throws IOException {
        SQLException sqlException = null;

        try {
            // try the action 2 times if a "recoverable" exception is thrown
            for (int i = 0; i < 2; i++) {
                try (Connection connection = dataSource.getConnection()) {
                    try {
                        R result = action.apply(connection);
                        connection.commit();
                        return result;
                    } catch (SQLRecoverableException ex) {
                        if (sqlException == null) {
                            sqlException = ex;
                        } else {
                            sqlException.addSuppressed(ex);
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            if (sqlException != null)
                ex.addSuppressed(sqlException);
            throw new IOException(ex);
        } catch (IOException | RuntimeException ex) {
            if (sqlException != null)
                ex.addSuppressed(sqlException);
            throw ex;
        }

        throw new IOException(sqlException);
    }

    @Override
    public void close() throws IOException {
        isClosed = true;
        if (dataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dataSource).close();
            } catch (IOException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IOException("Failed to close datasource!", ex);
            }
        }
    }

    private DataSource createDataSource(ConnectionFactory connectionFactory, int maxPoolSize) {
        ConnectionFactory retryingConnectionFactory = new RetryingConnectionFactory(
                () -> {
                    Logger.global.logDebug("Creating new SQL-Connection...");
                    return connectionFactory.createConnection();
                },
                5,
                1000,
                16000,
                2
        );

        PoolableConnectionFactory poolableConnectionFactory =
                new PoolableConnectionFactory(retryingConnectionFactory, null);
        poolableConnectionFactory.setPoolStatements(true);
        poolableConnectionFactory.setMaxOpenPreparedStatements(20);
        poolableConnectionFactory.setDefaultAutoCommit(false);
        poolableConnectionFactory.setAutoCommitOnReturn(false);
        poolableConnectionFactory.setRollbackOnReturn(true);
        poolableConnectionFactory.setFastFailValidation(true);

        GenericObjectPoolConfig<PoolableConnection> objectPoolConfig = new GenericObjectPoolConfig<>();
        objectPoolConfig.setTestWhileIdle(true);
        objectPoolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(10));
        objectPoolConfig.setNumTestsPerEvictionRun(3);
        objectPoolConfig.setBlockWhenExhausted(true);
        objectPoolConfig.setMinIdle(1);
        objectPoolConfig.setMaxIdle(Runtime.getRuntime().availableProcessors());
        objectPoolConfig.setMaxTotal(maxPoolSize);
        objectPoolConfig.setMaxWaitMillis(Duration.ofSeconds(30).toMillis());

        ObjectPool<PoolableConnection> connectionPool =
                new GenericObjectPool<>(poolableConnectionFactory, objectPoolConfig);
        poolableConnectionFactory.setPool(connectionPool);

        return new PoolingDataSource<>(connectionPool);
    }

    @FunctionalInterface
    public interface ConnectionConsumer extends ConnectionFunction<Void> {

        void accept(java.sql.Connection connection) throws SQLException, IOException;

        @Override
        default Void apply(java.sql.Connection connection) throws SQLException, IOException {
            accept(connection);
            return null;
        }

    }

    @FunctionalInterface
    public interface ConnectionFunction<R>  {

        R apply(java.sql.Connection connection) throws SQLException, IOException;

    }

}
