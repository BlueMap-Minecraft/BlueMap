package de.bluecolored.bluemap.core.storage.sql.dialect;

import de.bluecolored.bluemap.core.storage.sql.*;

public enum DialectType {

    MYSQL (MySQLStorage::new, "mysql"),
    MARIADB (MySQLStorage::new, "mariadb"),
    POSTGRESQL (PostgreSQLStorage::new, "postgresql"),
    SQLITE (SQLiteStorage::new, "sqlite");

    private static final DialectType FALLBACK = MYSQL;

    private final SQLStorageFactory storageFactory;
    private final String dialectName;

    DialectType(SQLStorageFactory storageFactory, String dialectName) {
        this.storageFactory = storageFactory;
        this.dialectName = dialectName;
    }
    public String getDialectName() {
        return dialectName;
    }

    public static SQLStorage getStorage(String dialectName, SQLStorageSettings settings) throws Exception {
        for (DialectType dialect : values()) {
            if (dialect.getDialectName().equals(dialectName)) {
                return dialect.storageFactory.provide(settings);
            }
        }

        // unknown dialect, use fallback
        return FALLBACK.storageFactory.provide(settings);
    }

    @FunctionalInterface
    public interface SQLStorageFactory {
        SQLStorage provide(SQLStorageSettings config) throws Exception;

    }

}
