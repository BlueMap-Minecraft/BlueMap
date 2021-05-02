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
package de.bluecolored.bluemap.core.map;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.core.util.AtomicFileHelper;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MapRenderState {

	private final Map<Vector2i, Long> regionRenderTimes;

	public MapRenderState() {
		regionRenderTimes = new HashMap<>();
	}

	public synchronized void setRenderTime(Vector2i regionPos, long renderTime) {
		regionRenderTimes.put(regionPos, renderTime);
	}

	public synchronized long getRenderTime(Vector2i regionPos) {
		Long renderTime = regionRenderTimes.get(regionPos);
		if (renderTime == null) return -1;
		else return renderTime;
	}

	public synchronized void save(File file) throws IOException {
		OutputStream fOut = AtomicFileHelper.createFilepartOutputStream(file);
		GZIPOutputStream gOut = new GZIPOutputStream(fOut);

		try (
				DataOutputStream dOut = new DataOutputStream(gOut)
		) {
			dOut.writeInt(regionRenderTimes.size());

			for (Map.Entry<Vector2i, Long> entry : regionRenderTimes.entrySet()) {
				Vector2i regionPos = entry.getKey();
				long renderTime = entry.getValue();

				dOut.writeInt(regionPos.getX());
				dOut.writeInt(regionPos.getY());
				dOut.writeLong(renderTime);
			}

			dOut.flush();
		}
	}

	public synchronized void load(File file) throws IOException {
		regionRenderTimes.clear();

		try (
				FileInputStream fIn = new FileInputStream(file);
				GZIPInputStream gIn = new GZIPInputStream(fIn);
				DataInputStream dIn = new DataInputStream(gIn)
		) {
			int size = dIn.readInt();

			for (int i = 0; i < size; i++) {
				Vector2i regionPos = new Vector2i(
						dIn.readInt(),
						dIn.readInt()
				);
				long renderTime = dIn.readLong();

				regionRenderTimes.put(regionPos, renderTime);
			}
		}
	}

}
