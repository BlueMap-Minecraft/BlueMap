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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.flowpowered.math.vector.Vector2i;

public class FileUtil {

	private FileUtil(){}
	
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
	 * Blocks until a file can be read and written.<br>
	 * <i>(Do not use this method to sync file-access from different threads!)</i>
	 */
	public static void waitForFile(File file, long time, TimeUnit unit) throws InterruptedException {
		long start = System.currentTimeMillis();
		long timeout = start + TimeUnit.MILLISECONDS.convert(time, unit);
		long sleepTime = 1;
		while(!file.canWrite() || !file.canRead()){
			Thread.sleep(sleepTime);
			sleepTime = (long) Math.min(Math.ceil(sleepTime * 1.5), 1000);
			if (System.currentTimeMillis() > timeout) throw new InterruptedException();
		}
	}
	
}
