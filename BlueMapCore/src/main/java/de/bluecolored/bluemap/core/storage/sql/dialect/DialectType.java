package de.bluecolored.bluemap.core.storage.sql.dialect;

import de.bluecolored.bluemap.core.storage.sql.MySQLStorage;
import de.bluecolored.bluemap.core.storage.sql.PostgreSQLStorage;
import de.bluecolored.bluemap.core.storage.sql.SQLStorage;
import de.bluecolored.bluemap.core.storage.sql.SQLStorageSettings;

public enum DialectType {

    MYSQL (MySQLStorage::new, "mysql"),
    MARIADB (MySQLStorage::new, "mariadb"),
    POSTGRESQL (PostgreSQLStorage::new,"postgresql");

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
        return null;
    }

    @FunctionalInterface
    public interface SQLStorageFactory {
        SQLStorage provide(SQLStorageSettings config) throws Exception;

    }

}
