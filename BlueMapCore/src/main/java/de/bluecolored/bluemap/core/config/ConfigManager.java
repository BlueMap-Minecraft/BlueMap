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


import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.util.FileUtils;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigManager {

	private static final Set<Placeholder> CONFIG_PLACEHOLDERS = new HashSet<>();
	
	static {
		CONFIG_PLACEHOLDERS.add(new Placeholder("version", BlueMap.VERSION));
		CONFIG_PLACEHOLDERS.add(new Placeholder("datetime-iso", () -> LocalDateTime.now().withNano(0).toString()));
	}

	/**
	 * Loads or creates a config file for BlueMap.
	 * 
	 * @param configFile The config file to load
	 * @param defaultConfig The default config that is used as a template if the config file does not exist (can be null)
	 * @param defaultValues The default values used if a key is not present in the config (can be null)
	 * @param usePlaceholders Whether to replace placeholders from the defaultConfig if it is newly generated 
	 * @param generateEmptyConfig Whether to generate an empty config file if no default config is provided
	 * @return The loaded configuration node
	 * @throws IOException if an IOException occurs while loading
	 */
	public ConfigurationNode loadOrCreate(File configFile, URL defaultConfig, URL defaultValues, boolean usePlaceholders, boolean generateEmptyConfig) throws IOException {
		
		ConfigurationNode configNode;
		if (!configFile.exists()) {
			FileUtils.mkDirsParent(configFile);
			
			if (defaultConfig != null) {
				//load content of default config
				String content;
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(defaultConfig.openStream(), StandardCharsets.UTF_8))){
					content = reader.lines().collect(Collectors.joining("\n"));
				}
	
				//replace placeholders if enabled
				if (usePlaceholders) {
					for (Placeholder placeholder : CONFIG_PLACEHOLDERS) {
						content = placeholder.apply(content);
					}
				}
	
				//create the config file
				Files.write(configFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
				
				//load
				configNode = getLoader(configFile).load();
			} else {
				//create empty config
				ConfigurationLoader<? extends ConfigurationNode> loader = getLoader(configFile);
				configNode = loader.createNode();
				
				//save to create file
				if (generateEmptyConfig) loader.save(configNode);
			}
		} else {
			//load config
			configNode = getLoader(configFile).load();
		}
		
		//populate missing values with default values
		if (defaultValues != null) {
			ConfigurationNode defaultValuesNode = getLoader(defaultValues).load();
			configNode.mergeFrom(defaultValuesNode);
		}
		
		return configNode;
	}
	
	private ConfigurationLoader<? extends ConfigurationNode> getLoader(URL url){
		if (url.getFile().endsWith(".json")) return GsonConfigurationLoader.builder().url(url).build();
		else return HoconConfigurationLoader.builder().url(url).build();
	}
	
	private ConfigurationLoader<? extends ConfigurationNode> getLoader(File file){
		if (file.getName().endsWith(".json")) return GsonConfigurationLoader.builder().file(file).build();
		else return HoconConfigurationLoader.builder().file(file).build();
	}

	public static File toFolder(String pathString) throws IOException {
		Objects.requireNonNull(pathString);
		
		File file = new File(pathString);
		if (file.exists() && !file.isDirectory()) throw new IOException("Invalid configuration: Path '" + file.getAbsolutePath() + "' is a file (should be a directory)");
		return file;
	}
	
}
