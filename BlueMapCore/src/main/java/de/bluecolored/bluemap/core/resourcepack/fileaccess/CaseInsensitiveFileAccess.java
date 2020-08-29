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

/**
 * This {@link FileAccess} maps its parent {@link FileAccess} to first look in assets/[namespace]/bluemap/... instead of assets/[namespace]/...
 */
public class CaseInsensitiveFileAccess implements FileAccess {

	public FileAccess parent;
	
	public CaseInsensitiveFileAccess(FileAccess parent) {
		this.parent = parent;
	}
	
	@Override
	public String getName() {
		return parent.getName() + "(CI)";
	}
	
	@Override
	public InputStream readFile(String path) throws FileNotFoundException, IOException {
		try {
			return parent.readFile(path);
		} catch (FileNotFoundException ex) {
			try {
				return parent.readFile(path.toLowerCase());
			} catch (FileNotFoundException ex2) {
				path = correctPathCase(path);
				return parent.readFile(path);
			}
		}
	}

	private String correctPathCase(String path) throws FileNotFoundException, IOException {
		path = FileAccess.normalize(path);
		String[] pathParts = path.split("/");
		
		String correctPath = "";
		for (int i = 0; i < pathParts.length; i++) {
			String part = correctPath + pathParts[i];
			
			boolean found = false;
			for(String folder : listFolders(correctPath)) {
				if (!folder.equalsIgnoreCase(part)) continue;
				
				part = folder;
				found = true;
				break;
			}
			
			if (!found && i == pathParts.length - 1) {
				for(String folder : listFiles(correctPath, false)) {
					if (!folder.equalsIgnoreCase(part)) continue;
					
					part = folder;
					found = true;
					break;
				}
			}
			
			if (!found) throw new FileNotFoundException();
			
			correctPath = part + "/";
		}
		
		correctPath = FileAccess.normalize(correctPath);
		return correctPath;
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
