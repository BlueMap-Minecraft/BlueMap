package de.bluecolored.bluemap.core.storage.sql;

import de.bluecolored.bluemap.core.storage.sql.dialect.MySQLDialect;

import java.net.MalformedURLException;

public class MySQLStorage extends SQLStorage{

    public MySQLStorage(SQLStorageSettings config) throws MalformedURLException, SQLDriverException {
        super(MySQLDialect.INSTANCE, config);
    }

}
