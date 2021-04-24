package de.bluecolored.bluemap.core.map;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.core.util.FileUtils;

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
		FileUtils.delete(file);
		FileUtils.createFile(file);

		try (
				FileOutputStream fOut = new FileOutputStream(file);
				GZIPOutputStream gOut = new GZIPOutputStream(fOut);
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
