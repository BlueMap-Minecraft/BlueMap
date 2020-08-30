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
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CombinedFileAccess implements FileAccess {

	private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
	
	public List<FileAccess> sources;
	private Map<String, FileAccess> sourceMap;
	
	private Set<String> allFiles;
	
	public CombinedFileAccess() {
		sources = new ArrayList<>();
		sourceMap = new HashMap<>();
		allFiles = new HashSet<>();
	}
	
	public void addFileAccess(FileAccess source) {
		rwLock.writeLock().lock();
		
		try {
			sources.add(source);
			
			Collection<String> sourceFiles = source.listFiles("", true);
			for (String path : sourceFiles) {
				sourceMap.put(FileAccess.normalize(path), source);
			}

			allFiles.addAll(sourceFiles);
		} finally {
			rwLock.writeLock().unlock();
		}
	}

	@Override
	public String getName() {
		return "CombinedFileAccess(" + sources.size() + ")";
	}
	
	@Override
	public InputStream readFile(String path) throws FileNotFoundException, IOException {
		rwLock.readLock().lock();
		try {
			FileAccess source = sourceMap.get(FileAccess.normalize(path));
			if (source != null) return source.readFile(path);
		} finally {
			rwLock.readLock().unlock();
		}
		
		throw new FileNotFoundException("File " + path + " does not exist in any of the sources!");
	}

	@Override
	public Collection<String> listFiles(String path, boolean recursive) {
		path = normalizeFolderPath(path);
		Collection<String> files = new ArrayList<String>();
		
		rwLock.readLock().lock();
		try {
			for (String file : allFiles) {
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
		} finally {
			rwLock.readLock().unlock();
		}
		
		return files;
	}

	private String normalizeFolderPath(String path) {
		if (path.isEmpty()) return path;
		if (path.charAt(path.length() - 1) != '/') path = path + "/";
		if (path.charAt(0) == '/') path = path.substring(1);
		return path;
	}
	
	/*
	@Override
	public Collection<String> listFiles(String path, boolean recursive) {
		Set<String> files = new HashSet<>();

		rwLock.readLock().lock();
		try {
			for (int i = 0; i < sources.size(); i++) {
				files.addAll(sources.get(i).listFiles(path, recursive));
			}
		} finally {
			rwLock.readLock().unlock();
		}
		
		return files;
	}
	*/

	@Override
	public Collection<String> listFolders(String path) {
		Set<String> folders = new HashSet<>();
		
		rwLock.readLock().lock();
		try {
			for (int i = 0; i < sources.size(); i++) {
				folders.addAll(sources.get(i).listFolders(path));
			}
		} finally {
			rwLock.readLock().unlock();
		}
		
		return folders;
	}
	
	@Override
	public void close() throws IOException {
		IOException exception = null;

		rwLock.writeLock().lock();
		try {
			for (FileAccess source : sources) {
				try {
					source.close();
				} catch (IOException ex) {
					if (exception == null) exception = ex;
					else exception.addSuppressed(ex);
				}
			}
		} finally {
			rwLock.writeLock().unlock();
		}
		
		if (exception != null) throw exception;
	}

}
