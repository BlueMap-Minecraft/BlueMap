package de.bluecolored.bluemap.common.plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Lists;

import de.bluecolored.bluemap.common.MapType;
import de.bluecolored.bluemap.common.RenderManager;
import de.bluecolored.bluemap.common.RenderTask;
import de.bluecolored.bluemap.common.plugin.serverinterface.CommandSource;
import de.bluecolored.bluemap.common.plugin.text.Text;
import de.bluecolored.bluemap.common.plugin.text.TextColor;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.mca.Chunk;
import de.bluecolored.bluemap.core.mca.ChunkAnvil112;
import de.bluecolored.bluemap.core.mca.MCAWorld;
import de.bluecolored.bluemap.core.render.hires.HiresModelManager;
import de.bluecolored.bluemap.core.world.Block;
import de.bluecolored.bluemap.core.world.World;

/**
 * Commands:
 * 
 * <ul>
 * <li>/bluemap</li>
 * <li>/bluemap reload</li>
 * <li>/bluemap pause</li>
 * <li>/bluemap resume</li>
 * <li>/bluemap render [world]</li>
 * <li>/bluemap render prioritize [task-uuid]</li>
 * <li>/bluemap render remove [task-uuid]</li>
 * <li>/bluemap debug</li>
 * </ul>
 */
public class Commands {
	
	private Plugin bluemap;
	
	public Commands(Plugin bluemap) {
		this.bluemap = bluemap;
	}
	
	/**
	 * Command: /bluemap
	 */
	public void executeRootCommand(CommandSource source) {
		if (!checkLoaded(source)) return;
		
		source.sendMessages(createStatusMessage());
	}
	
	/**
	 * Command: /bluemap debug
	 */
	public boolean executeDebugCommand(CommandSource source, UUID worldUuid, Vector3i playerPosition) {
		if (!checkLoaded(source)) return false;
		
		World world = bluemap.getWorld(worldUuid);

		if (world == null) {
			source.sendMessage(Text.of(TextColor.RED, "This world is not loaded with BlueMap! Maybe it is not configured?"));
			return false;
		}
		
		Block block = world.getBlock(playerPosition);
		Block blockBelow = world.getBlock(playerPosition.add(0, -1, 0));
		
		String blockIdMeta = "";
		String blockBelowIdMeta = "";
		
		if (world instanceof MCAWorld) {
			try {
				Chunk chunk = ((MCAWorld) world).getChunk(MCAWorld.blockToChunk(playerPosition));
				if (chunk instanceof ChunkAnvil112) {
					blockIdMeta = " (" + ((ChunkAnvil112) chunk).getBlockIdMeta(playerPosition) + ")";
					blockBelowIdMeta = " (" + ((ChunkAnvil112) chunk).getBlockIdMeta(playerPosition.add(0, -1, 0)) + ")";
				}
			} catch (IOException ex) {
				Logger.global.logError("Failed to read chunk for debug!", ex);
			}
		}
		
		source.sendMessages(Lists.newArrayList(
				Text.of(TextColor.GOLD, "Block at you: ", TextColor.WHITE, block, TextColor.GRAY, blockIdMeta),
				Text.of(TextColor.GOLD, "Block below you: ", TextColor.WHITE, blockBelow, TextColor.GRAY, blockBelowIdMeta)
			));
		
		return true;
	}

	/**
	 * Command: /bluemap reload
	 */
	public void executeReloadCommand(CommandSource source) {
		source.sendMessage(Text.of(TextColor.GOLD, "Reloading BlueMap..."));
		
		new Thread(() -> {
			try {
				bluemap.reload();
				
				if (bluemap.isLoaded()) {
					source.sendMessage(Text.of(TextColor.GREEN, "BlueMap reloaded!"));
				} else {
					source.sendMessage(Text.of(TextColor.RED, "Could not load BlueMap! See the console for details!"));
				}

			} catch (Exception ex) {
				Logger.global.logError("Failed to reload BlueMap!", ex);
				
				source.sendMessage(Text.of(TextColor.RED, "There was an error reloading BlueMap! See the console for details!"));
			}
		}).start();

	}

	/**
	 * Command: /bluemap pause
	 */
	public boolean executePauseCommand(CommandSource source) {
		if (!checkLoaded(source)) return false;
		
		if (bluemap.getRenderManager().isRunning()) {
			bluemap.getRenderManager().stop();
			source.sendMessage(Text.of(TextColor.GREEN, "BlueMap rendering paused!"));
			return true;
		} else {
			source.sendMessage(Text.of(TextColor.RED, "BlueMap rendering are already paused!"));
			return false;
		}
	}

	/**
	 * Command: /bluemap resume
	 */
	public boolean executeResumeCommand(CommandSource source) {
		if (!checkLoaded(source)) return false;
		
		if (!bluemap.getRenderManager().isRunning()) {
			bluemap.getRenderManager().start();
			source.sendMessage(Text.of(TextColor.GREEN, "BlueMap renders resumed!"));
			return true;
		} else {
			source.sendMessage(Text.of(TextColor.RED, "BlueMap renders are already running!"));
			return false;
		}
	}


	/**
	 * Command: /bluemap render [world]
	 */
	public boolean executeRenderWorldCommand(CommandSource source, UUID worldUuid) {
		return executeRenderWorldCommand(source, worldUuid, null, -1);
	}
	
	/**
	 * Command: /bluemap render [world] [block-radius]
	 */
	public boolean executeRenderWorldCommand(CommandSource source, UUID worldUuid, Vector2i center, int blockRadius) {
		if (!checkLoaded(source)) return false;
		
		World world = bluemap.getWorld(worldUuid);
		
		if (world == null) {
			source.sendMessage(Text.of(TextColor.RED, "This world is not loaded with BlueMap! Maybe it is not configured?"));
			return false;
		}
		
		world.invalidateChunkCache();
		
		new Thread(() -> {
			createWorldRenderTask(source, world, center, blockRadius);
		}).start();
		
		return true;
	}

	/**
	 * Command: /bluemap render prioritize [task-uuid]
	 */
	public void executePrioritizeRenderTaskCommand(CommandSource source, UUID taskUUID) {
		if (!checkLoaded(source)) return;
		
		for (RenderTask task : bluemap.getRenderManager().getRenderTasks()) {
			if (task.getUuid().equals(taskUUID)) {
				bluemap.getRenderManager().prioritizeRenderTask(task);
				break;
			}
		}
		
		source.sendMessages(createStatusMessage());
	}
	
	/**
	 * Command: /bluemap render remove [task-uuid]
	 */
	public void executeRemoveRenderTaskCommand(CommandSource source, UUID taskUUID) {
		if (!checkLoaded(source)) return;
		
		for (RenderTask task : bluemap.getRenderManager().getRenderTasks()) {
			if (task.getUuid().equals(taskUUID)) {
				bluemap.getRenderManager().removeRenderTask(task);
				break;
			}
		}
		
		source.sendMessages(createStatusMessage());
	}
	
	private List<Text> createStatusMessage(){
		List<Text> lines = new ArrayList<>();
		
		RenderManager renderer = bluemap.getRenderManager();
		
		lines.add(Text.of());
		lines.add(Text.of(TextColor.BLUE, "Tile-Updates:"));
		
		if (renderer.isRunning()) {
			lines.add(Text.of(TextColor.WHITE, " Render-Threads are ", Text.of(TextColor.GREEN, "running").setHoverText(Text.of("click to pause rendering")).setClickCommand("/bluemap pause"), TextColor.GRAY, "!"));
		} else {
			lines.add(Text.of(TextColor.WHITE, " Render-Threads are ", Text.of(TextColor.RED, "paused").setHoverText(Text.of("click to resume rendering")).setClickCommand("/bluemap resume"), TextColor.GRAY, "!"));
		}
		
		lines.add(Text.of(TextColor.WHITE, " Scheduled tile-updates: ", Text.of(TextColor.GOLD, renderer.getQueueSize()).setHoverText(Text.of("tiles waiting for a free render-thread")), TextColor.GRAY, " + " , Text.of(TextColor.GRAY, bluemap.getUpdateHandler().getUpdateBufferCount()).setHoverText(Text.of("tiles waiting for world-save"))));
		
		RenderTask[] tasks = renderer.getRenderTasks();
		if (tasks.length > 0) {
			RenderTask task = tasks[0];
			
			long time = task.getActiveTime();
			String durationString = DurationFormatUtils.formatDurationWords(time, true, true);
			double pct = (double)task.getRenderedTileCount() / (double)(task.getRenderedTileCount() + task.getRemainingTileCount());
			
			long ert = (long)((time / pct) * (1d - pct));
			String ertDurationString = DurationFormatUtils.formatDurationWords(ert, true, true);

			double tps = task.getRenderedTileCount() / (time / 1000.0);
			
			lines.add(Text.of(TextColor.BLUE, "Current task:"));
			lines.add(Text.of(" ", createCancelTaskText(task), TextColor.WHITE, " Task ", TextColor.GOLD, task.getName(), TextColor.WHITE, " for map ", Text.of(TextColor.GOLD, task.getMapType().getName()).setHoverText(Text.of(TextColor.WHITE, "World: ", TextColor.GOLD, task.getMapType().getWorld().getName()))));
			lines.add(Text.of(TextColor.WHITE, " rendered ", TextColor.GOLD, task.getRenderedTileCount(), TextColor.WHITE, " tiles ", TextColor.GRAY, "(" + (Math.round(pct * 1000)/10.0) + "% | " + GenericMath.round(tps, 1) + "t/s)", TextColor.WHITE, " in ", TextColor.GOLD, durationString));
			lines.add(Text.of(TextColor.WHITE, " with ", TextColor.GOLD, task.getRemainingTileCount(), TextColor.WHITE, " tiles to go. ETA: ", TextColor.GOLD, ertDurationString));
		}

		if (tasks.length > 1) {
			lines.add(Text.of(TextColor.BLUE, "Waiting tasks:"));
			for (int i = 1; i < tasks.length; i++) {
				RenderTask task = tasks[i];
				lines.add(Text.of(" ", createCancelTaskText(task), createPrioritizeTaskText(task), TextColor.WHITE, " Task ", TextColor.GOLD, task.getName(), TextColor.WHITE, " for map ", Text.of(TextColor.GOLD, task.getMapType().getName()).setHoverText(Text.of(TextColor.WHITE, "World: ", TextColor.GOLD, task.getMapType().getWorld().getName())), TextColor.GRAY, " (" + task.getRemainingTileCount() + " tiles)"));
			}
		}
		
		return lines;
	}
	
	private Text createCancelTaskText(RenderTask task) {
		return Text.of(TextColor.RED, "[X]").setHoverText(Text.of(TextColor.GRAY, "click to remove this render-task")).setClickCommand("/bluemap render remove " + task.getUuid());
	}
	
	private Text createPrioritizeTaskText(RenderTask task) {
		return Text.of(TextColor.GREEN, "[^]").setHoverText(Text.of(TextColor.GRAY, "click to prioritize this render-task")).setClickCommand("/bluemap render prioritize " + task.getUuid());
	}
	
	private void createWorldRenderTask(CommandSource source, World world, Vector2i center, long blockRadius) {
		source.sendMessage(Text.of(TextColor.GOLD, "Collecting chunks to render..."));
		
		String taskName = "world-render";
		
		Predicate<Vector2i> filter;
		if (center == null || blockRadius < 0) {
			filter = c -> true;
		} else {
			filter = c -> c.mul(16).distanceSquared(center) <= blockRadius * blockRadius;
			taskName = "radius-render";
		}
		
		Collection<Vector2i> chunks = world.getChunkList(filter);
		
		source.sendMessage(Text.of(TextColor.GREEN, chunks.size() + " chunks found!"));
		
		for (MapType map : bluemap.getMapTypes()) {
			if (!map.getWorld().getUUID().equals(world.getUUID())) continue;

			source.sendMessage(Text.of(TextColor.GOLD, "Collecting tiles for map '" + map.getId() + "'"));
			
			HiresModelManager hmm = map.getTileRenderer().getHiresModelManager();
			Collection<Vector2i> tiles = hmm.getTilesForChunks(chunks);
			
			RenderTask task = new RenderTask(taskName, map);
			task.addTiles(tiles);
			task.optimizeQueue();
			bluemap.getRenderManager().addRenderTask(task);
			
			source.sendMessage(Text.of(TextColor.GREEN, tiles.size() + " tiles found! Task created."));
		}

		source.sendMessage(Text.of(TextColor.GREEN, "All render tasks created! Use /bluemap to view the progress!"));
	}
	
	private boolean checkLoaded(CommandSource source) {
		if (!bluemap.isLoaded()) {
			source.sendMessage(Text.of(TextColor.RED, "BlueMap is not loaded!", TextColor.GRAY, "(Try /bluemap reload)"));
			return false;
		}
		
		return true;
	}
	
}
