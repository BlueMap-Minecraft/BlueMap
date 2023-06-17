package de.bluecolored.bluemap.core.storage.sql;

import de.bluecolored.bluemap.core.storage.sql.dialect.SqliteDialect;

import java.net.MalformedURLException;

public class SQLiteStorage extends PostgreSQLStorage {

    public SQLiteStorage(SQLStorageSettings config) throws MalformedURLException, SQLDriverException {
        super(SqliteDialect.INSTANCE, config);
    }

}
