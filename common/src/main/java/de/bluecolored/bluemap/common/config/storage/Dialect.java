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

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public interface Dialect extends Keyed {

    Dialect MYSQL = new Impl(Key.bluemap("mysql"), "jdbc:mysql:", MySQLCommandSet::new, List.of());
    Dialect MARIADB = new Impl(Key.bluemap("mariadb"), "jdbc:mariadb:", MySQLCommandSet::new, List.of());
    Dialect POSTGRESQL = new Impl(Key.bluemap("postgresql"), "jdbc:postgresql:", PostgreSQLCommandSet::new, List.of(
            "SET synchronous_commit = off"
    ));
    Dialect SQLITE = new Impl(Key.bluemap("sqlite"), "jdbc:sqlite:", SqliteCommandSet::new, List.of(
            "PRAGMA journal_mode = WAL",
            "PRAGMA synchronous = NORMAL",
            "PRAGMA busy_timeout = 30000",
            "PRAGMA foreign_keys = ON"
    ));

    Registry<Dialect> REGISTRY = new Registry<>(
            MYSQL,
            MARIADB,
            POSTGRESQL,
            SQLITE
    );

    boolean supports(String connectionUrl);

    CommandSet createCommandSet(Database database);

    Collection<String> getConnectionInitSql();

    @RequiredArgsConstructor
    class Impl implements Dialect {

        @Getter private final Key key;
        private final String protocol;

        private final Function<Database, CommandSet> commandSetProvider;
        @Getter private final Collection<String> connectionInitSql;

        @Override
        public boolean supports(String connectionUrl) {
            return connectionUrl.startsWith(protocol);
        }

        @Override
        public CommandSet createCommandSet(Database database) {
            return commandSetProvider.apply(database);
        }

    }

}
