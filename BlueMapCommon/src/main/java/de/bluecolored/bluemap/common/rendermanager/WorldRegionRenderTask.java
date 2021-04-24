package de.bluecolored.bluemap.common.rendermanager;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.world.Grid;
import de.bluecolored.bluemap.core.world.Region;

import java.util.Collection;
import java.util.TreeSet;

public class WorldRegionRenderTask implements RenderTask {

	private final BmMap map;
	private final Vector2i worldRegion;
	private final boolean force;

	private TreeSet<Vector2i> tiles;
	private int tileCount;
	private long startTime;

	private volatile int atWork;
	private volatile boolean cancelled;

	public WorldRegionRenderTask(BmMap map, Vector2i worldRegion) {
		this(map, worldRegion, false);
	}

	public WorldRegionRenderTask(BmMap map, Vector2i worldRegion, boolean force) {
		this.map = map;
		this.worldRegion = worldRegion;
		this.force = force;

		this.tiles = null;
		this.tileCount = -1;
		this.startTime = -1;

		this.atWork = 0;
		this.cancelled = false;
	}

	private synchronized void init() {
		tiles = new TreeSet<>();
		startTime = System.currentTimeMillis();

		long changesSince = 0;
		if (!force) changesSince = map.getRenderState().getRenderTime(worldRegion);

		Region region = map.getWorld().getRegion(worldRegion.getX(), worldRegion.getY());
		Collection<Vector2i> chunks = region.listChunks(changesSince);

		Grid tileGrid = map.getHiresModelManager().getTileGrid();
		Grid chunkGrid = map.getWorld().getChunkGrid();

		for (Vector2i chunk : chunks) {
			Vector2i tileMin = chunkGrid.getCellMin(chunk, tileGrid);
			Vector2i tileMax = chunkGrid.getCellMax(chunk, tileGrid);

			for (int x = tileMin.getX(); x < tileMax.getX(); x++) {
				for (int z = tileMin.getY(); z < tileMax.getY(); z++) {
					tiles.add(new Vector2i(x, z));
				}
			}
		}

		this.tileCount = tiles.size();

		if (tiles.isEmpty()) complete();
	}

	@Override
	public void doWork() {
		if (cancelled) return;

		Vector2i tile;

		synchronized (this) {
			if (tiles == null) init();
			if (tiles.isEmpty()) return;

			tile = tiles.pollFirst();

			this.atWork++;
		}

		map.renderTile(tile); // <- actual work

		synchronized (this) {
			this.atWork--;

			if (atWork <= 0 && tiles.isEmpty() && !cancelled) {
				complete();
			}
		}
	}

	private void complete() {
		map.getRenderState().setRenderTime(worldRegion, startTime);
	}

	@Override
	public boolean hasMoreWork() {
		return !cancelled && !tiles.isEmpty();
	}

	@Override
	public double estimateProgress() {
		if (tiles == null) return 0;
		if (tileCount == 0) return 1;

		double remainingTiles = tiles.size();
		return remainingTiles / this.tileCount;
	}

	@Override
	public void cancel() {
		this.cancelled = true;

		synchronized (this) {
			if (tiles != null) this.tiles.clear();
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		WorldRegionRenderTask that = (WorldRegionRenderTask) o;
		return force == that.force && map.equals(that.map) && worldRegion.equals(that.worldRegion);
	}

	@Override
	public int hashCode() {
		return worldRegion.hashCode();
	}

}
