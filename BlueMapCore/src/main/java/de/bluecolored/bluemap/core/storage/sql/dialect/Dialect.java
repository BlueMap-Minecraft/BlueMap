package de.bluecolored.bluemap.core.storage.sql.dialect;

import de.bluecolored.bluemap.core.storage.sql.MySQLStorage;
import de.bluecolored.bluemap.core.storage.sql.PostgreSQLStorage;
import de.bluecolored.bluemap.core.storage.sql.SQLStorage;
import de.bluecolored.bluemap.core.storage.sql.SQLStorageSettings;

public enum Dialect {
    MySQL( MySQLStorage.class, "mysql" ),
    MariaDB( MySQLStorage.class, "mariadb" ),
    PostgreSQL( PostgreSQLStorage.class,"postgresql");

    private final Class<? extends SQLStorage> storageClass;
    private final String dialectName;


    <C extends SQLStorage> Dialect(Class<C> storageClass, String dialectName) {
        this.storageClass = storageClass;
        this.dialectName = dialectName;
    }
    public String getDialectName() {
        return dialectName;
    }

    public static SQLStorage getStorage(String dialectName, SQLStorageSettings settings){
        for (Dialect dialect : values()) {
            if (dialect.getDialectName().equals(dialectName)) {
                try {
                    return dialect.storageClass.getConstructor(SQLStorageSettings.class).newInstance(settings);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
