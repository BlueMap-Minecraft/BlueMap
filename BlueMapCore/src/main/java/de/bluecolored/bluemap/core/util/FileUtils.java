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
package de.bluecolored.bluemap.core.util;

import com.flowpowered.math.vector.Vector2i;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

public class FileUtils {

	private FileUtils(){}

	public static void delete(File file) throws IOException {
		if (file.exists()) org.apache.commons.io.FileUtils.forceDelete(file);
	}

	public static void mkDirs(File directory) throws IOException {
		org.apache.commons.io.FileUtils.forceMkdir(directory);
	}

	public static void mkDirsParent(File file) throws IOException {
		org.apache.commons.io.FileUtils.forceMkdirParent(file);
	}

	public static void createFile(File file) throws IOException {
		if (!file.exists()) {
			org.apache.commons.io.FileUtils.forceMkdirParent(file);
			if (!file.createNewFile()) throw new IOException("Could not create file '" + file + "'!");
		} else {
			if (!file.isFile()) throw new IOException("File '" + file + "' exists but is not a normal file!");
		}
	}

	public static File coordsToFile(Path root, Vector2i coords, String fileType){
		String path = "x" + coords.getX() + "z" + coords.getY();
		char[] cs = path.toCharArray();
		List<String> folders = new ArrayList<>();
		String folder = "";
		for (char c : cs){
			folder += c;
			if (c >= '0' && c <= '9'){
				folders.add(folder);
				folder = "";
			}
		}
		String fileName = folders.remove(folders.size() - 1);
		
		Path p = root;
		for (String s : folders){
			p = p.resolve(s);
		}
		
		return p.resolve(fileName + "." + fileType).toFile();
	}
	
	/**
	 * The path-elements are being matched to the pattern-elements, 
	 * each pattern-element can be a regex pattern to match against one path-element or "*" to represent any number of arbitrary elements (lazy: until the next pattern matches).
	 */
	public static boolean matchPath(Path path, String... pattern) {
		int p = 0;
		for (int i = 0; i < path.getNameCount(); i++) {
			while (pattern[p].equals("*")) {
				p++;
				
				if (pattern.length >= p) return true;
			}

			if (Pattern.matches(pattern[p], path.getName(i).toString())) {
				p++;
				continue;
			}
			
			if (p > 0 && pattern[p-1].equals("*")) continue;
			
			return false;
		}
		
		return true;
	}
	
}
