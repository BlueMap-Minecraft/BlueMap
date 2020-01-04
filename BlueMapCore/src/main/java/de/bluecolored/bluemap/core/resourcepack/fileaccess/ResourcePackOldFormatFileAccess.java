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
import java.util.List;
import java.util.regex.Pattern;

/**
 * This {@link FileAccess} tries to make 1.12/1.13/1.14 ResourcePacks compatible with each other 
 */
public class ResourcePackOldFormatFileAccess implements FileAccess {

	private FileAccess parent;
	
	protected ResourcePackOldFormatFileAccess(FileAccess parent) {
		this.parent = parent;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public InputStream readFile(String path) throws FileNotFoundException, IOException {
		try {
			return parent.readFile(path);
		} catch (FileNotFoundException ex) {
			for (String altPath : otherPathsToTry(path)) {
				try {
					return parent.readFile(altPath);
				} catch (FileNotFoundException ex2) {
					ex.addSuppressed(ex2);
				} catch (IOException ex2) {
					ex.addSuppressed(ex2);
					throw ex;
				}
			}
			
			throw ex;
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
	
	private Collection<String> otherPathsToTry(String path){
		path = FileAccess.normalize(path);
		List<String> paths = new ArrayList<>();
		String[] parts = path.split(Pattern.quote("/"));

		//handle block/blocks folder-differences
		if (parts.length >= 4 && parts[0].equals("assets") && parts[2].equals("models")) {
			if (parts[3].equals("block")) {
				parts[3] = "blocks";
				paths.add(String.join("/", parts));
			} else if (parts[3].equals("blocks")) {
				parts[3] = "block";
				paths.add(String.join("/", parts));
			} else {
				String[] newParts = new String[parts.length + 1];
				System.arraycopy(parts, 0, newParts, 0, 3);
				System.arraycopy(parts, 3, newParts, 4, parts.length - 3);
				
				newParts[3] = "blocks";
				paths.add(String.join("/", newParts));
	
				newParts[3] = "block";
				paths.add(String.join("/", newParts));
			}
		}
		
		return paths;
	}
	
	public static ResourcePackOldFormatFileAccess from(FileAccess source) {
		if (source instanceof ResourcePackOldFormatFileAccess) return (ResourcePackOldFormatFileAccess) source;
		return new ResourcePackOldFormatFileAccess(source);
	}
	
}
