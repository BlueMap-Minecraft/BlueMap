package de.bluecolored.bluemap.core.storage.sql.dialect;

public enum Dialect {
    MariaDB(new MariaDBFactory()),
    PostgreSQL(new PostgresFactory());

    private final SQLQueryFactory dialectFactory;


    Dialect(SQLQueryFactory storageFactory) {
        this.dialectFactory = storageFactory;
    }

    public SQLQueryFactory getDialectFactory() {
        return dialectFactory;
    }
}
