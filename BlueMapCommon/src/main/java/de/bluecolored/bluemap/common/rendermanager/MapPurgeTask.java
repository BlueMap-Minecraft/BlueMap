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
package de.bluecolored.bluemap.common.rendermanager;

import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.util.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class MapPurgeTask implements RenderTask {

	private final BmMap map;
	private final Path directory;
	private final int subFilesCount;
	private final LinkedList<Path> subFiles;

	private volatile boolean hasMoreWork;
	private volatile boolean cancelled;

	public MapPurgeTask(Path mapDirectory) throws IOException {
		this(null, mapDirectory);
	}

	public MapPurgeTask(BmMap map) throws IOException {
		this(map, map.getFileRoot());
	}

	private MapPurgeTask(BmMap map, Path directory) throws IOException {
		this.map = map;
		this.directory = directory;
		this.subFiles = Files.walk(directory, 3)
				.collect(Collectors.toCollection(LinkedList::new));
		this.subFilesCount = subFiles.size();
		this.hasMoreWork = true;
		this.cancelled = false;
	}

	@Override
	public void doWork() throws Exception {
		synchronized (this) {
			if (!this.hasMoreWork) return;
			this.hasMoreWork = false;
		}

		// delete subFiles first to be able to track the progress and cancel
		while (!subFiles.isEmpty()) {
			Path subFile = subFiles.getLast();
			FileUtils.delete(subFile.toFile());
			subFiles.removeLast();
			if (this.cancelled) return;
		}

		// make sure everything is deleted
		FileUtils.delete(directory.toFile());

		// reset map render state
		if (this.map != null) {
			this.map.getRenderState().reset();
		}
	}

	@Override
	public boolean hasMoreWork() {
		return this.hasMoreWork;
	}

	@Override
	public double estimateProgress() {
		return 1d - (subFiles.size() / (double) subFilesCount);
	}

	@Override
	public void cancel() {
		this.cancelled = true;
	}

	@Override
	public boolean contains(RenderTask task) {
		if (task == this) return true;
		if (task instanceof MapPurgeTask) {
			return ((MapPurgeTask) task).directory.toAbsolutePath().normalize().startsWith(this.directory.toAbsolutePath().normalize());
		}

		return false;
	}

	@Override
	public String getDescription() {
		return "Purge Map " + directory.getFileName();
	}

}
