package de.bluecolored.bluemap.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.flowpowered.math.vector.Vector2i;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;

import de.bluecolored.bluemap.core.logger.Logger;

public class RenderManager {

	private boolean running;
	
	private Thread[] renderThreads;
	private ArrayDeque<RenderTicket> renderTickets;
	private Map<RenderTicket, RenderTicket> renderTicketMap;
	private Deque<RenderTask> renderTasks;
	
	public RenderManager(int threadCount) {
		running = false;
		renderThreads = new Thread[threadCount];
		renderTickets = new ArrayDeque<>(1000);
		renderTicketMap = new HashMap<>(1000);
		renderTasks = new ArrayDeque<>();
	}
	
	public synchronized void start() {
		stop(); //ensure everything is stopped first
		
		for (int i = 0; i < renderThreads.length; i++) {
			renderThreads[i] = new Thread(this::renderThread);
			renderThreads[i].setDaemon(true);
			renderThreads[i].setPriority(Thread.MIN_PRIORITY);
			renderThreads[i].start();
		}
		
		running = true;
	}
	
	public synchronized void stop() {
		for (int i = 0; i < renderThreads.length; i++) {
			if (renderThreads[i] != null) {
				renderThreads[i].interrupt();
				renderThreads[i] = null;
			}
		}
		
		running = false;
	}
	
	public void addRenderTask(RenderTask task) {
		synchronized (renderTasks) {
			renderTasks.add(task);
		}
	}
	
	public RenderTicket createTicket(MapType mapType, Vector2i tile) {
		RenderTicket ticket = new RenderTicket(mapType, tile);
		synchronized (renderTickets) {
			if (renderTicketMap.putIfAbsent(ticket, ticket) == null) {
				renderTickets.add(ticket);
				return ticket;
			} else {
				return renderTicketMap.get(ticket);
			}
		}
	}
	
	public Collection<RenderTicket> createTickets(MapType mapType, Collection<Vector2i> tiles) {
		if (tiles.size() < 0) return Collections.emptyList();
		
		Collection<RenderTicket> tickets = new ArrayList<>(tiles.size());
		synchronized (renderTickets) {
			for (Vector2i tile : tiles) {
				tickets.add(createTicket(mapType, tile));
			}
		}
		
		return tickets;
	}
	
	public boolean prioritizeRenderTask(RenderTask renderTask) {
		synchronized (renderTasks) {
			if (renderTasks.remove(renderTask)) {
				
				//pause first task
				RenderTask currentFirst = renderTasks.peek();
				if (currentFirst != null) currentFirst.pause();
				
				renderTasks.addFirst(renderTask);
				return true;
			}
			
			return false;
		}
	}
	
	public boolean removeRenderTask(RenderTask renderTask) {
		synchronized (renderTasks) {
			return renderTasks.remove(renderTask);
		}
	}
	
	private void renderThread() {
		RenderTicket ticket = null;
		
		while (!Thread.interrupted()) {
			synchronized (renderTickets) {
				ticket = renderTickets.poll();
				if (ticket != null) renderTicketMap.remove(ticket);
			}
			
			if (ticket == null) {
				synchronized (renderTasks) {
					RenderTask task = renderTasks.peek();
					if (task != null) {
						ticket = task.poll();
						if (task.isFinished()) renderTasks.poll();
						task.getMapType().getTileRenderer().save();
					}
				}
			}
			
			if (ticket != null) {
				try {
					ticket.render();
				} catch (IOException e) {
					Logger.global.logError("Failed to render tile " + ticket.getTile() + " of map '" + ticket.getMapType().getId() + "'!", e);
				}
			} else {
				try {
					Thread.sleep(1000); // we don't need a super fast response time, so waiting a second is totally fine
				} catch (InterruptedException e) { break; }
			}
		}
	}
	
	public int getQueueSize() {
		return renderTickets.size();
	}
	
	/**
	 * Returns a copy of the deque with the render tasks in order as array
	 */
	public RenderTask[] getRenderTasks(){
		return renderTasks.toArray(new RenderTask[renderTasks.size()]);
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public void writeState(DataOutputStream out) throws IOException {
		//prepare renderTickets
		ListMultimap<MapType, Vector2i> tileMap = MultimapBuilder.hashKeys().arrayListValues().<MapType, Vector2i>build();
		synchronized (renderTickets) {
			for (RenderTicket ticket : renderTickets) {
				tileMap.put(ticket.getMapType(), ticket.getTile());
			}	
		}
		
		//write renderTickets
		Set<MapType> maps = tileMap.keySet();
		out.writeInt(maps.size());
		for (MapType map : maps) {
			List<Vector2i> tiles = tileMap.get(map);
			
			out.writeUTF(map.getId());
			out.writeInt(tiles.size());
			for (Vector2i tile : tiles) {
				out.writeInt(tile.getX());
				out.writeInt(tile.getY());
			}
		}
		
		//write tasks
		synchronized (renderTasks) {
			out.writeInt(renderTasks.size());
			for (RenderTask task : renderTasks) {
				task.write(out);
			}
		}
	}
	
	public void readState(DataInputStream in, Collection<MapType> mapTypes) throws IOException {
		//read renderTickets
		int mapCount = in.readInt();
		for (int i = 0; i < mapCount; i++) {
			String mapId = in.readUTF();
			
			MapType mapType = null;
			for (MapType map : mapTypes) {
				if (map.getId().equals(mapId)) {
					mapType = map;
					break;
				}
			}
			if (mapType == null) {
				Logger.global.logWarning("Some render-tickets can not be loaded because the map (id: '" + mapId + "') does not exist anymore. They will be discarded.");
			}
			
			int tileCount = in.readInt();
			List<Vector2i> tiles = new ArrayList<>();
			for (int j = 0; j < tileCount; j++) {
				int x = in.readInt();
				int y = in.readInt();
				Vector2i tile = new Vector2i(x, y);
				tiles.add(tile);
			}
			
			createTickets(mapType, tiles);
		}
		
		//read tasks
		int taskCount = in.readInt();
		for (int i = 0; i < taskCount; i++) {
			try {
				RenderTask task = RenderTask.read(in, mapTypes);
				addRenderTask(task);
			} catch (IOException ex) {
				Logger.global.logWarning("A render-task can not be loaded. It will be discared. (Error message: " + ex.toString() + ")");
			}
		}
	}
	
}
