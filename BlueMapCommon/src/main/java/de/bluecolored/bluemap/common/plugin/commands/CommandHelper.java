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

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.text.Text;
import de.bluecolored.bluemap.common.plugin.text.TextColor;
import de.bluecolored.bluemap.common.rendermanager.RenderManager;
import de.bluecolored.bluemap.common.rendermanager.RenderTask;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.world.Grid;
import de.bluecolored.bluemap.core.world.World;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class CommandHelper {

	private final Plugin plugin;
	
	public CommandHelper(Plugin plugin) {
		this.plugin = plugin;
	}
	
	public List<Text> createStatusMessage(){
		List<Text> lines = new ArrayList<>();

		RenderManager renderer = plugin.getRenderManager();
		List<RenderTask> tasks = renderer.getScheduledRenderTasks();

		lines.add(Text.of(TextColor.BLUE, "BlueMap - Status:"));

		if (renderer.isRunning()) {
			Text status;
			if (tasks.isEmpty()) {
				status = Text.of(TextColor.GRAY, "idle");
			} else {
				status = Text.of(TextColor.GREEN, "running");
			}

			status.setHoverText(Text.of("click to stop rendering"));
			status.setClickAction(Text.ClickAction.RUN_COMMAND, "/bluemap stop");

			lines.add(Text.of(TextColor.WHITE, " Render-Threads are ", status, TextColor.WHITE, "!"));

			if (!tasks.isEmpty()) {
				lines.add(Text.of(TextColor.WHITE, " Queued Tasks (" + tasks.size() + "):"));
				for (int i = 0; i < tasks.size(); i++) {
					if (i >= 10){
						lines.add(Text.of(TextColor.GRAY, "..."));
						break;
					}

					RenderTask task = tasks.get(i);
					lines.add(Text.of(TextColor.GRAY, " - ", TextColor.GOLD, task.getDescription()));

					if (i == 0) {
						lines.add(Text.of(TextColor.GRAY, "    Progress: ", TextColor.WHITE,
								(Math.round(task.estimateProgress() * 10000) / 100.0) + "%"));
						lines.add(Text.of(TextColor.GRAY, "    ETA: ", TextColor.WHITE, DurationFormatUtils.formatDuration(renderer.estimateCurrentRenderTaskTimeRemaining(), "HH:mm:ss")));
					}
				}
			}
		} else {
			lines.add(Text.of(TextColor.WHITE, " Render-Threads are ",
					Text.of(TextColor.RED, "stopped")
							.setHoverText(Text.of("click to start rendering"))
							.setClickAction(Text.ClickAction.RUN_COMMAND, "/bluemap start"),
					TextColor.GRAY, "!"));

			if (!tasks.isEmpty()) {
				lines.add(Text.of(TextColor.WHITE, " Queued Tasks (" + tasks.size() + "):"));
				for (int i = 0; i < tasks.size(); i++) {
					if (i >= 10){
						lines.add(Text.of(TextColor.GRAY, "..."));
						break;
					}

					RenderTask task = tasks.get(i);
					lines.add(Text.of(TextColor.GRAY, " - ", TextColor.WHITE, task.getDescription()));
				}
			}
		}

		return lines;
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
		for (BmMap map : plugin.getMapTypes()) {
			joiner.add(map.getId());
		}
		
		return Text.of("map").setHoverText(Text.of(TextColor.WHITE, "Available maps: \n", TextColor.GRAY, joiner.toString()));
	}

	public List<Vector2i> getRegions(World world, Vector2i center, int radius) {
		if (center == null || radius < 0) return new ArrayList<>(world.listRegions());

		List<Vector2i> regions = new ArrayList<>();

		Grid regionGrid = world.getRegionGrid();
		Vector2i halfCell = regionGrid.getGridSize().div(2);
		int increasedRadiusSquared = (int) Math.pow(radius + Math.ceil(halfCell.length()), 2);

		for (Vector2i region : world.listRegions()) {
			Vector2i min = regionGrid.getCellMin(region);
			Vector2i regionCenter = min.add(halfCell);

			if (regionCenter.distanceSquared(center) <= increasedRadiusSquared)
				regions.add(region);
		}

		return regions;
	}

}
