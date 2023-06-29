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
