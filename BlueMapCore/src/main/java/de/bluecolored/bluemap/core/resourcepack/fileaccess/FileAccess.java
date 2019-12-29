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

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public interface FileAccess extends Closeable, AutoCloseable {

	InputStream readFile(String path) throws FileNotFoundException, IOException;
	
	Collection<String> listFiles(String path, boolean recursive);

	Collection<String> listFolders(String path);
	
	static FileAccess of(File file) throws IOException {
		if (file.isDirectory()) return new FolderFileAccess(file);
		if (file.isFile()) return new ZipFileAccess(file);
		throw new IOException("Unsupported file!");
	}
	
	static String getFileName(String path) {
		String filename = path;
		
		int nameSplit = path.lastIndexOf('/');
		if (nameSplit > -1) {
			filename = path.substring(nameSplit + 1);
		}
		
		return filename;
	}

	static String normalize(String path) {
		if (path.charAt(path.length() - 1) == '/') path = path.substring(0, path.length() - 1);
		if (path.charAt(0) == '/') path = path.substring(1);
		return path;
	}
	
}
