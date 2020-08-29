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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CombinedFileAccess implements FileAccess {

	public List<FileAccess> sources;
	private Map<String, FileAccess> sourceMap;
	
	public CombinedFileAccess() {
		sources = new ArrayList<>();
		sourceMap = new HashMap<>();
	}
	
	public void addFileAccess(FileAccess source) {
		synchronized (sources) {
			sources.add(source);
		}
		synchronized (sourceMap) {
			for (String path : source.listFiles("", true)) {
				sourceMap.put(FileAccess.normalize(path), source);
			}
		}
	}

	@Override
	public String getName() {
		return "CombinedFileAccess(" + sources.size() + ")";
	}
	
	@Override
	public InputStream readFile(String path) throws FileNotFoundException, IOException {
		synchronized (sourceMap) {
			FileAccess source = sourceMap.get(FileAccess.normalize(path));
			if (source != null) return source.readFile(path);
		}
		
		throw new FileNotFoundException("File " + path + " does not exist in any of the sources!");
	}

	@Override
	public Collection<String> listFiles(String path, boolean recursive) {
		Set<String> files = new HashSet<>();

		synchronized (sources) {
			for (int i = 0; i < sources.size(); i++) {
				files.addAll(sources.get(i).listFiles(path, recursive));
			}
		}
		
		return files;
	}

	@Override
	public Collection<String> listFolders(String path) {
		Set<String> folders = new HashSet<>();

		synchronized (sources) {
			for (int i = 0; i < sources.size(); i++) {
				folders.addAll(sources.get(i).listFolders(path));
			}
		}
		
		return folders;
	}
	
	@Override
	public void close() throws IOException {
		IOException exception = null;

		synchronized (sources) {
			for (FileAccess source : sources) {
				try {
					source.close();
				} catch (IOException ex) {
					if (exception == null) exception = ex;
					else exception.addSuppressed(ex);
				}
			}
		}
		
		if (exception != null) throw exception;
	}

}
