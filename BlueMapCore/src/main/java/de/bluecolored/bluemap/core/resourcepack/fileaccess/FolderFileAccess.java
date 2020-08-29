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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class FolderFileAccess implements FileAccess {

	private File folder;
	private Collection<WeakReference<InputStream>> openedStreams;
	
	public FolderFileAccess(File folder) {
		this.folder = folder;
		openedStreams = new ArrayList<>();
	}
	
	@Override
	public String getName() {
		return folder.getName();
	}
	
	@Override
	public synchronized InputStream readFile(String path) throws FileNotFoundException {
		InputStream stream = new FileInputStream(resolve(path).toFile());
		tidy();
		openedStreams.add(new WeakReference<>(stream));
		return stream;
	}

	@Override
	public Collection<String> listFiles(String path, boolean recursive) {
		File subFolder = resolve(path).toFile();
		List<String> paths = new ArrayList<>();
		listFiles(subFolder, paths, recursive);
		return paths;
	}
	
	private void listFiles(File folder, Collection<String> paths, boolean recursive) {
		if (!folder.isDirectory()) return;
		for (File file : folder.listFiles()) {
			if (recursive && file.isDirectory()) listFiles(file, paths, true);
			if (file.isFile()) paths.add(toPath(file));
		}
	}
	
	@Override
	public Collection<String> listFolders(String path) {
		File subFolder = resolve(path).toFile();
		List<String> paths = new ArrayList<>();
		
		if (subFolder.isDirectory()) {
			for (File file : subFolder.listFiles()) {
				if (file.isDirectory()) paths.add(toPath(file));
			}
		}
		
		return paths;
	}
	
	@Override
	public synchronized void close() throws IOException {
		IOException exception = null;
		
		for (WeakReference<InputStream> streamRef : openedStreams) {
			try {
				InputStream stream = streamRef.get();
				if (stream != null) stream.close();
				streamRef.clear();
			} catch (IOException ex) {
				if (exception == null) exception = ex;
				else exception.addSuppressed(ex);
			}
		}
		
		if (exception != null) throw exception;
		
		openedStreams.clear(); //only clear if no exception is thrown
	}
	
	private synchronized void tidy() {
		Iterator<WeakReference<InputStream>> iterator = openedStreams.iterator();
		while (iterator.hasNext()) {
			WeakReference<InputStream> ref = iterator.next();
			if (ref.get() == null) iterator.remove();
		}
	}
	
	private Path resolve(String path) {
		if (path.isEmpty() || "/".equals(path)) return folder.toPath();
		if (File.separatorChar != '/') path = path.replace('/', File.separatorChar);
		if (path.charAt(0) == '/') path = path.substring(1);
		Path resolve = folder.toPath();
		for (String s : path.split("/")) {
			resolve = resolve.resolve(s);
		}
		return resolve;
	}
	
	private String toPath(File file) {
		return toPath(file.toPath());
	}
	
	private String toPath(Path path) {
		return folder
				.toPath()
				.relativize(path)
				.normalize()
				.toString()
				.replace(File.separatorChar, '/');
	}

}
