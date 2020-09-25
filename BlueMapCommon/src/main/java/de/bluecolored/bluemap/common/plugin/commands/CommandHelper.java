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
package de.bluecolored.bluemap.common.plugin.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Predicate;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector2i;

import de.bluecolored.bluemap.common.MapType;
import de.bluecolored.bluemap.common.RenderManager;
import de.bluecolored.bluemap.common.RenderTask;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.serverinterface.CommandSource;
import de.bluecolored.bluemap.common.plugin.text.Text;
import de.bluecolored.bluemap.common.plugin.text.TextColor;
import de.bluecolored.bluemap.common.plugin.text.TextFormat;
import de.bluecolored.bluemap.core.render.hires.HiresModelManager;
import de.bluecolored.bluemap.core.world.World;

public class CommandHelper {

	private Plugin plugin;
	
	public CommandHelper(Plugin plugin) {
		this.plugin = plugin;
	}
	
	public List<Text> createStatusMessage(){
		List<Text> lines = new ArrayList<>();
		
		RenderManager renderer = plugin.getRenderManager();
		
		lines.add(Text.of());
		lines.add(Text.of(TextColor.BLUE, "Tile-Updates:"));
		
		if (renderer.isRunning()) {
			lines.add(Text.of(TextColor.WHITE, " Render-Threads are ", 
					Text.of(TextColor.GREEN, "running")
					.setHoverText(Text.of("click to pause rendering"))
					.setClickCommand("/bluemap pause"), 
					TextColor.GRAY, "!"));
		} else {
			lines.add(Text.of(TextColor.WHITE, " Render-Threads are ", 
					Text.of(TextColor.RED, "paused")
					.setHoverText(Text.of("click to resume rendering"))
					.setClickCommand("/bluemap resume"),
					TextColor.GRAY, "!"));
		}
		
		lines.add(Text.of(
				TextColor.WHITE, " Scheduled tile-updates: ", 
				TextColor.GOLD, renderer.getQueueSize()).setHoverText(
						Text.of(
								TextColor.WHITE, "Tiles waiting for a free render-thread: ", TextColor.GOLD, renderer.getQueueSize(), 
								TextColor.WHITE, "\n\nChunks marked as changed: ", TextColor.GOLD, plugin.getUpdateHandler().getUpdateBufferCount(),
								TextColor.GRAY, TextFormat.ITALIC, "\n(Changed chunks will be rendered as soon as they are saved back to the world-files)"
								)
						)
				);
		
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
		return Text.of(TextColor.RED, "[X]").setHoverText(Text.of(TextColor.GRAY, "click to cancel this render-task")).setClickCommand("/bluemap render cancel " + task.getUuid());
	}
	
	private Text createPrioritizeTaskText(RenderTask task) {
		return Text.of(TextColor.GREEN, "[^]").setHoverText(Text.of(TextColor.GRAY, "click to prioritize this render-task")).setClickCommand("/bluemap render prioritize " + task.getUuid());
	}
	
	public void createWorldRenderTask(CommandSource source, World world, Vector2i center, long blockRadius) {
		
		for (MapType map : plugin.getMapTypes()) {
			if (!map.getWorld().getUUID().equals(world.getUUID())) continue;

			createMapRenderTask(source, map, center, blockRadius);
		}

		source.sendMessage(Text.of(TextColor.GREEN, "All render tasks created! Use /bluemap to view the progress!"));
	}
	
	public void createMapRenderTask(CommandSource source, MapType map, Vector2i center, long blockRadius) {
		source.sendMessage(Text.of(TextColor.GOLD, "Creating render-task for map: " + map.getId()));
		source.sendMessage(Text.of(TextColor.GOLD, "Collecting chunks..."));
		
		String taskName = "world-render";
		Vector2i renderCenter = map.getWorld().getSpawnPoint().toVector2(true);
		
		Predicate<Vector2i> filter;
		if (center == null || blockRadius < 0) {
			filter = c -> true;
		} else {
			filter = c -> c.mul(16).distanceSquared(center) <= blockRadius * blockRadius;
			taskName = "radius-render";
			renderCenter = center;
		}
		
		Collection<Vector2i> chunks = map.getWorld().getChunkList(filter);
		
		source.sendMessage(Text.of(TextColor.GREEN, chunks.size() + " chunks found!"));
		source.sendMessage(Text.of(TextColor.GOLD, "Collecting tiles..."));
		
		HiresModelManager hmm = map.getTileRenderer().getHiresModelManager();
		Collection<Vector2i> tiles = hmm.getTilesForChunks(chunks);
		
		RenderTask task = new RenderTask(taskName, map);
		task.addTiles(tiles);
		task.optimizeQueue(renderCenter);
		plugin.getRenderManager().addRenderTask(task);
		
		source.sendMessage(Text.of(TextColor.GREEN, tiles.size() + " tiles found! Task created."));
	}
	
	public Text worldHelperHover() {
		StringJoiner joiner = new StringJoiner("\n");
		for (World world : plugin.getWorlds()) {
			joiner.add(world.getName());
		}
		
		return Text.of("world").setHoverText(Text.of(TextColor.WHITE, "Available worlds: \n", TextColor.GRAY, joiner.toString()));
	}
	
	public Text mapHelperHover() {
		StringJoiner joiner = new StringJoiner("\n");
		for (MapType map : plugin.getMapTypes()) {
			joiner.add(map.getId());
		}
		
		return Text.of("map").setHoverText(Text.of(TextColor.WHITE, "Available maps: \n", TextColor.GRAY, joiner.toString()));
	}
	
	public boolean checkLoaded(CommandSource source) {
		if (!plugin.isLoaded()) {
			source.sendMessage(Text.of(TextColor.RED, "BlueMap is not loaded!", TextColor.GRAY, "(Try /bluemap reload)"));
			return false;
		}
		
		return true;
	}

	public boolean checkPermission(CommandSource source, String permission) {
		if (source.hasPermission(permission)) return true;
		
		source.sendMessage(Text.of(TextColor.RED, "You don't have the permissions to use this command!"));
		return false;
	}
	
}
