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
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

/**
 * This {@link FileAccess} maps its parent {@link FileAccess} to first look in assets/[namespace]/bluemap/... instead of assets/[namespace]/...
 */
public class BluemapAssetOverrideFileAccess implements FileAccess {

	public FileAccess parent;
	
	public BluemapAssetOverrideFileAccess(FileAccess parent) {
		this.parent = parent;
	}
	
	@Override
	public InputStream readFile(String path) throws FileNotFoundException, IOException {
		String[] pathParts = StringUtils.split(path, "/");
		if (pathParts.length < 3 || !pathParts[0].equals("assets")) return parent.readFile(path);
		
		String[] newParts = new String[pathParts.length + 1];
		System.arraycopy(pathParts, 0, newParts, 0, 2);
		System.arraycopy(pathParts, 2, newParts, 3, pathParts.length - 2);
		
		newParts[2] = "bluemap";
		String newPath = String.join("/", newParts);
		
		try {
			return parent.readFile(newPath);
		} catch (FileNotFoundException ex) {
			return parent.readFile(path);
		}
	}

	@Override
	public Collection<String> listFiles(String path, boolean recursive) {
		return parent.listFiles(path, recursive);
	}

	@Override
	public Collection<String> listFolders(String path) {
		return parent.listFolders(path);
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

}
