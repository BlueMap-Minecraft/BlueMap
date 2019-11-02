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
package de.bluecolored.bluemap.sponge;

import java.nio.file.Path;

import javax.inject.Inject;

import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.plugin.Plugin;

import de.bluecolored.bluemap.core.logger.Logger;

@Plugin(
		id = SpongePlugin.PLUGIN_ID, 
		name = SpongePlugin.PLUGIN_NAME,
		authors = { "Blue (Lukas Rieger)" },
		description = "This plugin provides a fully 3D map of your world for your browser!",
		version = SpongePlugin.PLUGIN_VERSION
		)
public class SpongePlugin {

	public static final String PLUGIN_ID = "bluemap";
	public static final String PLUGIN_NAME = "BlueMap";
	public static final String PLUGIN_VERSION = "0.0.0";

	private static Object plugin;
	
	@Inject
	@ConfigDir(sharedRoot = false)
	private Path configurationDir;
	
	@Inject
	public SpongePlugin(org.slf4j.Logger logger) {
		plugin = this;
		
		Logger.global = new Slf4jLogger(logger);
	}
	
	public Path getConfigPath(){
		return configurationDir;
	}

	public static Object getPlugin() {
		return plugin;
	}
	
}
