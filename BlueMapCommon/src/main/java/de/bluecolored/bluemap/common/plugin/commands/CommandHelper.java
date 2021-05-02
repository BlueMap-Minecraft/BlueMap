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

import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.text.Text;
import de.bluecolored.bluemap.common.plugin.text.TextColor;
import de.bluecolored.bluemap.common.rendermanager.RenderManager;
import de.bluecolored.bluemap.common.rendermanager.RenderTask;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.world.World;

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

		lines.add(Text.of(TextColor.BLUE, "BlueMap - Status:"));

		if (renderer.isRunning()) {
			lines.add(Text.of(TextColor.WHITE, " Render-Threads are ",
					Text.of(TextColor.GREEN, "running")
							.setHoverText(Text.of("click to pause rendering"))
							.setClickAction(Text.ClickAction.RUN_COMMAND, "/bluemap pause"),
					TextColor.GRAY, "!"));
		} else {
			lines.add(Text.of(TextColor.WHITE, " Render-Threads are ",
					Text.of(TextColor.RED, "paused")
							.setHoverText(Text.of("click to resume rendering"))
							.setClickAction(Text.ClickAction.RUN_COMMAND, "/bluemap resume"),
					TextColor.GRAY, "!"));
		}

		List<RenderTask> tasks = renderer.getScheduledRenderTasks();
		lines.add(Text.of(TextColor.WHITE, " Scheduled tasks: ", TextColor.GOLD, tasks.size()));

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
}
