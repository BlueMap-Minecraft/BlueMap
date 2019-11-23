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
package de.bluecolored.bluemap.core.config;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.resourcepack.ResourcePack;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

public class ConfigurationFile {
	
	private static final Set<Placeholder> CONFIG_PLACEHOLDERS = new HashSet<>();
	
	static {
		CONFIG_PLACEHOLDERS.add(new Placeholder("version", BlueMap.VERSION));
		CONFIG_PLACEHOLDERS.add(new Placeholder("datetime-iso", () -> LocalDateTime.now().withNano(0).toString()));
		CONFIG_PLACEHOLDERS.add(new Placeholder("minecraft-client-url", ResourcePack.MINECRAFT_CLIENT_URL));
		CONFIG_PLACEHOLDERS.add(new Placeholder("minecraft-client-version", ResourcePack.MINECRAFT_CLIENT_VERSION));
	}
	
	private File configFile;
	private Configuration config;
	
	private ConfigurationFile(File configFile) throws IOException {
		this.configFile = configFile;
		
		ConfigurationLoader<CommentedConfigurationNode> configLoader = HoconConfigurationLoader.builder()
				.setFile(configFile)
				.build();
		
		CommentedConfigurationNode rootNode = configLoader.load();
		
		this.config = new Configuration(rootNode);
	}
	
	public File getFile() {
		return configFile;
	}
	
	public Configuration getConfig() {
		return config;
	}
	
	public static ConfigurationFile loadOrCreate(File configFile) throws IOException {
		return loadOrCreate(configFile, ConfigurationFile.class.getResource("/bluemap.conf"));
	}
	
	public static ConfigurationFile loadOrCreate(File configFile, URL defaultConfig) throws IOException {
		if (!configFile.exists()) {
			configFile.getParentFile().mkdirs();
			
			FileUtils.copyURLToFile(defaultConfig, configFile, 10000, 10000);
			
			//replace placeholder
			String content = new String(Files.readAllBytes(configFile.toPath()), StandardCharsets.UTF_8);
			for (Placeholder placeholder : CONFIG_PLACEHOLDERS) {
				content = placeholder.apply(content);
			}
			Files.write(configFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
		}
		
		return new ConfigurationFile(configFile);
	}
	
	public static void registerPlaceholder(Placeholder placeholder) {
		CONFIG_PLACEHOLDERS.add(placeholder);
	}
	
}
