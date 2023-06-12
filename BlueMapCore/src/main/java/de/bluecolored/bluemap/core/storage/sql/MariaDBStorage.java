package de.bluecolored.bluemap.core.storage.sql;

import de.bluecolored.bluemap.core.storage.sql.dialect.MariaDBFactory;

import java.net.MalformedURLException;

public class MariaDBStorage extends SQLStorage{
    public MariaDBStorage(SQLStorageSettings config) throws MalformedURLException, SQLDriverException {
        super(new MariaDBFactory(), config);
    }
}
