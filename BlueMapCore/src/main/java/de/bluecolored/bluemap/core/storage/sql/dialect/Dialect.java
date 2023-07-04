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

import org.intellij.lang.annotations.Language;

public interface Dialect {
    @Language("sql")
    String writeMapTile();

    @Language("sql")
    String readMapTile();

    @Language("sql")
    String readMapTileInfo();

    @Language("sql")
    String deleteMapTile();

    @Language("sql")
    String writeMeta();

    @Language("sql")
    String readMeta();

    @Language("sql")
    String readMetaSize();

    @Language("sql")
    String deleteMeta();

    @Language("sql")
    String purgeMapTile();

    @Language("sql")
    String purgeMapMeta();

    @Language("sql")
    String purgeMap();

    @Language("sql")
    String selectMapIds();

    @Language("sql")
    String initializeStorageMeta();

    @Language("sql")
    String selectStorageMeta();

    @Language("sql")
    String insertStorageMeta();

    @Language("sql")
    String initializeMap();

    @Language("sql")
    String initializeMapTileCompression();

    @Language("sql")
    String initializeMapMeta();

    @Language("sql")
    String initializeMapTile();

    @Language("sql")
    String updateStorageMeta(); // can be use twice in init

    @Language("sql")
    String deleteMapMeta();

    @Language("sql")
    String updateMapMeta(); // can be used twice in init

    @Language("sql")
    String lookupFK(String table, String idField, String valueField);

    @Language("sql")
    String insertFK(String table, String valueField);

}
