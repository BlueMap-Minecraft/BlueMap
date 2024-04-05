package de.bluecolored.bluemap.common.config.storage;

import de.bluecolored.bluemap.core.storage.sql.Database;
import de.bluecolored.bluemap.core.storage.sql.commandset.CommandSet;
import de.bluecolored.bluemap.core.storage.sql.commandset.MySQLCommandSet;
import de.bluecolored.bluemap.core.storage.sql.commandset.PostgreSQLCommandSet;
import de.bluecolored.bluemap.core.storage.sql.commandset.SqliteCommandSet;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

public interface Dialect extends Keyed {

    Dialect MYSQL = new Impl(Key.bluemap("mysql"), MySQLCommandSet::new);
    Dialect MARIADB = new Impl(Key.bluemap("mariadb"), MySQLCommandSet::new);
    Dialect POSTGRESQL = new Impl(Key.bluemap("postgresql"), PostgreSQLCommandSet::new);
    Dialect SQLITE = new Impl(Key.bluemap("sqlite"), SqliteCommandSet::new);

    Registry<Dialect> REGISTRY = new Registry<>(
            MYSQL,
            MARIADB,
            POSTGRESQL,
            SQLITE
    );

    CommandSet createCommandSet(Database database);

    @RequiredArgsConstructor
    class Impl implements Dialect {

        @Getter
        private final Key key;

        private final Function<Database, CommandSet> commandSetProvider;

        @Override
        public CommandSet createCommandSet(Database database) {
            return commandSetProvider.apply(database);
        }

    }

}
