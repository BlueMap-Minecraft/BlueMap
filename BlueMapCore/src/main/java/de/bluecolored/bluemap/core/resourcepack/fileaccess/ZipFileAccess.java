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
package de.bluecolored.bluemap.core.resourcepack.fileaccess;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ZipFileAccess implements FileAccess {

    private ZipFile file;

    public ZipFileAccess(File file) throws ZipException, IOException {
        this.file = new ZipFile(file);
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public InputStream readFile(String path) throws FileNotFoundException, IOException {
        ZipEntry entry = file.getEntry(path);
        if (entry == null) throw new FileNotFoundException("File " + path + " does not exist in this zip-file!");

        return file.getInputStream(entry);
    }

    @Override
    public Collection<String> listFiles(String path, boolean recursive) {
        path = normalizeFolderPath(path);

        Collection<String> files = new ArrayList<String>();
        for (Enumeration<? extends ZipEntry> entries = file.entries(); entries.hasMoreElements();) {
            ZipEntry entry = entries.nextElement();

            if (entry.isDirectory()) continue;

            String file = entry.getName();
            int nameSplit = file.lastIndexOf('/');
            String filePath = "";
            if (nameSplit != -1) {
                filePath = file.substring(0, nameSplit);
            }
            filePath = normalizeFolderPath(filePath);

            if (recursive) {
                if (!filePath.startsWith(path) && !path.equals(filePath)) continue;
            } else {
                if (!path.equals(filePath)) continue;
            }

            files.add(file);
        }

        return files;
    }

    @Override
    public Collection<String> listFolders(String path) {
        path = normalizeFolderPath(path);

        Collection<String> folders = new HashSet<String>();
        for (Enumeration<? extends ZipEntry> entries = file.entries(); entries.hasMoreElements();) {
            ZipEntry entry = entries.nextElement();

            String file = entry.getName();
            if (!entry.isDirectory()) {
                int nameSplit = file.lastIndexOf('/');
                if (nameSplit == -1) continue;
                file = file.substring(0, nameSplit);
            }
            file = normalizeFolderPath(file);

            //strip last /
            file = file.substring(0, file.length() - 1);

            int nameSplit = file.lastIndexOf('/');
            String filePath = "/";
            if (nameSplit != -1) {
                filePath = file.substring(0, nameSplit);
            }
            filePath = normalizeFolderPath(filePath);

            if (!filePath.startsWith(path)) continue;

            int subFolderMark = file.indexOf('/', path.length());
            if (subFolderMark != -1) {
                file = file.substring(0, subFolderMark);
            }

            file = normalizeFolderPath(file);

            //strip last /
            file = file.substring(0, file.length() - 1);

            folders.add(file);
        }

        return folders;
    }

    private String normalizeFolderPath(String path) {
        if (path.isEmpty()) return path;
        if (path.charAt(path.length() - 1) != '/') path = path + "/";
        if (path.charAt(0) == '/') path = path.substring(1);
        return path;
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

}
