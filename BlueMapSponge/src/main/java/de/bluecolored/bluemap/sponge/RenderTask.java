package de.bluecolored.bluemap.sponge;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.UUID;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector2i;

public class RenderTask {

	private final UUID uuid;
	private String name;
	
	private final MapType mapType;
	private Deque<Vector2i> renderTiles;

	private long firstTileTime;
	private long additionalRunTime;
	private int renderedTiles;
	
	private UUID taskOwner;
	
	public RenderTask(String name, MapType mapType) {
		this.uuid = UUID.randomUUID();
		this.name = name;
		this.mapType = mapType;
		this.renderTiles = new ArrayDeque<>();
		this.firstTileTime = -1;
		this.additionalRunTime = 0;
		this.renderedTiles = 0;
		this.taskOwner = null;
	}
	
	public void optimizeQueue() {
		//Find a good grid size to match the MCAWorlds chunk-cache size of 500
		Vector2d sortGridSize = new Vector2d(20, 20).div(mapType.getTileRenderer().getHiresModelManager().getTileSize().toDouble().div(16)).ceil().max(1, 1);
		
		synchronized (renderTiles) {
			Vector2i[] array = renderTiles.toArray(new Vector2i[renderTiles.size()]);
			Arrays.sort(array, (v1, v2) -> {
				Vector2i v1SortGridPos = v1.toDouble().div(sortGridSize).floor().toInt();
				Vector2i v2SortGridPos = v2.toDouble().div(sortGridSize).floor().toInt();
				
				if (v1SortGridPos != v2SortGridPos){
					int v1Dist = v1SortGridPos.distanceSquared(Vector2i.ZERO);
					int v2Dist = v2SortGridPos.distanceSquared(Vector2i.ZERO);
					
					if (v1Dist < v2Dist) return -1;
					if (v1Dist > v2Dist) return 1;
				}
				
				if (v1.getY() < v1.getY()) return -1;
				if (v1.getY() > v1.getY()) return 1;
				if (v1.getX() < v1.getX()) return -1;
				if (v1.getX() > v1.getX()) return 1;
				
				return 0;
			});
			renderTiles.clear();
			for (Vector2i tile : array) {
				renderTiles.add(tile);
			}
		}
	}
	
	public void addTile(Vector2i tile) {
		synchronized (renderTiles) {
			renderTiles.add(tile);
		}
	}
	
	public void addTiles(Collection<Vector2i> tiles) {
		synchronized (renderTiles) {
			renderTiles.addAll(tiles);
		}
	}
	
	public RenderTicket poll() {
		synchronized (renderTiles) {
			Vector2i tile = renderTiles.poll();
			if (tile != null) {
				renderedTiles++;
				if (firstTileTime < 0) firstTileTime = System.currentTimeMillis();
				return new RenderTicket(mapType, tile);
			} else {
				return null;
			}
		}
	}
	
	/**
	 * Pauses the render-time counter.
	 * So if the rendering gets paused, the statistics remain correct.
	 * It will resume as soon as a new ticket gets polled
	 */
	public void pause() {
		synchronized (renderTiles) {
			additionalRunTime += System.currentTimeMillis() - firstTileTime;
			firstTileTime = -1;	
		}
	}
	
	public long getActiveTime() {
		if (firstTileTime < 0) return additionalRunTime;
		return (System.currentTimeMillis() - firstTileTime) + additionalRunTime;
	}
	
	public UUID getUuid() {
		return uuid;
	}
	
	public String getName() {
		return name;
	}
	
	public MapType getMapType() {
		return mapType;
	}
	
	public int getRenderedTileCount() {
		return renderedTiles;
	}
	
	public int getRemainingTileCount() {
		return renderTiles.size();
	}
	
	public boolean isFinished() {
		return renderTiles.isEmpty();
	}
	
	public UUID getTaskOwner() {
		return taskOwner;
	}
	
}
