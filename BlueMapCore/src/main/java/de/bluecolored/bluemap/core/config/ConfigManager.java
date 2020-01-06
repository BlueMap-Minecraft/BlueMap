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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resourcepack.ResourcePack.Resource;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

public class ConfigManager {

	private static final Set<Placeholder> CONFIG_PLACEHOLDERS = new HashSet<>();
	
	static {
		CONFIG_PLACEHOLDERS.add(new Placeholder("version", BlueMap.VERSION));
		CONFIG_PLACEHOLDERS.add(new Placeholder("datetime-iso", () -> LocalDateTime.now().withNano(0).toString()));
		CONFIG_PLACEHOLDERS.add(new Placeholder("minecraft-client-url", ResourcePack.MINECRAFT_CLIENT_URL));
		CONFIG_PLACEHOLDERS.add(new Placeholder("minecraft-client-version", ResourcePack.MINECRAFT_CLIENT_VERSION));
	}
	
	private File configFolder;
	
	private URL defaultMainConfig;
	private URL mainConfigDefaultValues;
	
	private MainConfig mainConfig;
	private BlockIdConfig blockIdConfig;
	private BlockPropertiesConfig blockPropertiesConfig;
	private BiomeConfig biomeConfig;
	
	/**
	 * Manages all configurations BlueMap needs to render stuff.
	 * 
	 * @param configFolder The folder containing all configuration-files
	 * @param defaultMainConfig The default main-configuration file, used if a new configuration is generated 
	 * @param mainConfigDefaultValues The default values that are used for the main-configuration file (if they are undefined)
	 */
	public ConfigManager(File configFolder, URL defaultMainConfig, URL mainConfigDefaultValues) {
		this.defaultMainConfig = defaultMainConfig;
		this.configFolder = configFolder;
	}
	
	public MainConfig getMainConfig() {
		return mainConfig;
	}

	public File getMainConfigFile() {
		return new File(configFolder, "bluemap.conf");
	}
	
	public BlockIdConfig getBlockIdConfig() {
		return blockIdConfig;
	}
	
	public File getBlockIdConfigFile() {
		return new File(configFolder, "blockIds.json");
	}

	public BlockPropertiesConfig getBlockPropertiesConfig() {
		return blockPropertiesConfig;
	}
	
	public File getBlockPropertiesConfigFile() {
		return new File(configFolder, "blockProperties.json");
	}

	public BiomeConfig getBiomeConfig() {
		return biomeConfig;
	}
	
	public File getBiomeConfigFile() {
		return new File(configFolder, "biomes.json");
	}
	
	public File getBlockColorConfigFile() {
		return new File(configFolder, "blockColors.json");
	}
	
	public void loadMainConfig() throws IOException {
		mainConfig = new MainConfig(
				loadOrCreate(
						getMainConfigFile(), 
						defaultMainConfig, 
						mainConfigDefaultValues, 
						true, 
						true
						)
				);	
	}
	
	public void loadResourceConfigs(ResourcePack resourcePack) throws IOException {
		
		//load blockColors.json from resources, config-folder and resourcepack
		URL blockColorsConfigUrl = BlueMap.class.getResource("/blockColors.json");
		ConfigurationNode blockColorsConfigNode = loadOrCreate(
				getBlockColorConfigFile(), 
				null, 
				blockColorsConfigUrl, 
				false,
				false
				);
		blockColorsConfigNode = joinFromResourcePack(resourcePack, "blockColors.json", blockColorsConfigNode); 
		resourcePack.getBlockColorCalculator().loadColorConfig(blockColorsConfigNode);

		//load blockIds.json from resources, config-folder and resourcepack
		URL blockIdsConfigUrl = BlueMap.class.getResource("/blockIds.json");
		ConfigurationNode blockIdsConfigNode = loadOrCreate(
						getBlockIdConfigFile(), 
						null, 
						blockIdsConfigUrl,
						false,
						false
						);
		blockIdsConfigNode = joinFromResourcePack(resourcePack, "blockIds.json", blockIdsConfigNode);
		blockIdConfig = new BlockIdConfig(
				blockIdsConfigNode, 
				null //getLoader(makeAutogen(getBlockIdConfigFile()))
				);

		//load blockProperties.json from resources, config-folder and resourcepack
		URL blockPropertiesConfigUrl = BlueMap.class.getResource("/blockProperties.json");
		ConfigurationNode blockPropertiesConfigNode = loadOrCreate(
						getBlockPropertiesConfigFile(), 
						null,
						blockPropertiesConfigUrl,
						false, 
						false
						);
		blockPropertiesConfigNode = joinFromResourcePack(resourcePack, "blockProperties.json", blockPropertiesConfigNode);
		blockPropertiesConfig = new BlockPropertiesConfig(
				blockPropertiesConfigNode,
				resourcePack,
				null //getLoader(makeAutogen(getBlockPropertiesConfigFile()))
				);

		//load biomes.json from resources, config-folder and resourcepack
		URL biomeConfigUrl = BlueMap.class.getResource("/biomes.json");
		ConfigurationNode biomeConfigNode = loadOrCreate(
						getBiomeConfigFile(),
						null, 
						biomeConfigUrl,
						false,
						false
						);
		biomeConfigNode = joinFromResourcePack(resourcePack, "biomes.json", biomeConfigNode);
		biomeConfig = new BiomeConfig(
				biomeConfigNode,
				null //getLoader(makeAutogen(getBiomeConfigFile()))
				);
	}
	
	private ConfigurationNode joinFromResourcePack(ResourcePack resourcePack, String configFileName, ConfigurationNode defaultConfig) {
		ConfigurationNode joinedNode = null;
		for (Resource resource : resourcePack.getConfigAdditions(configFileName)) {
			try {
				ConfigurationNode node = getLoader(configFileName, resource.read()).load();
				if (joinedNode == null) joinedNode = node;
				else joinedNode.mergeValuesFrom(node);
			} catch (IOException ex) {
				Logger.global.logWarning("Failed to load an additional " + configFileName + " from the resource-pack! " + ex);
			}
		}
		
		if (joinedNode == null) return defaultConfig;
		
		joinedNode.mergeValuesFrom(defaultConfig);
		
		return joinedNode;
	}
	
	private ConfigurationNode loadOrCreate(File configFile, URL defaultConfig, URL defaultValues, boolean usePlaceholders, boolean generateEmptyConfig) throws IOException {
		
		ConfigurationNode configNode;
		if (!configFile.exists()) {
			configFile.getParentFile().mkdirs();
			
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
				configNode = loader.createEmptyNode();
				
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
			configNode.mergeValuesFrom(defaultValuesNode);
		}
		
		return configNode;
	}
	
	/*
	private File makeAutogen(File file) throws IOException {
		File autogenFile = file.getCanonicalFile().toPath().getParent().resolve("generated").resolve(file.getName()).toFile();
		autogenFile.getParentFile().mkdirs();
		return autogenFile;
	}
	*/
	
	private ConfigurationLoader<? extends ConfigurationNode> getLoader(String filename, InputStream is){
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
		
		if (filename.endsWith(".json")) return GsonConfigurationLoader.builder().setSource(() -> reader).build();
		if (filename.endsWith(".yaml") || filename.endsWith(".yml")) return YAMLConfigurationLoader.builder().setSource(() -> reader).build();
		else return HoconConfigurationLoader.builder().setSource(() -> reader).build();
	}
	
	private ConfigurationLoader<? extends ConfigurationNode> getLoader(URL url){
		if (url.getFile().endsWith(".json")) return GsonConfigurationLoader.builder().setURL(url).build();
		if (url.getFile().endsWith(".yaml") || url.getFile().endsWith(".yml")) return YAMLConfigurationLoader.builder().setURL(url).build();
		else return HoconConfigurationLoader.builder().setURL(url).build();
	}
	
	private ConfigurationLoader<? extends ConfigurationNode> getLoader(File file){
		if (file.getName().endsWith(".json")) return GsonConfigurationLoader.builder().setFile(file).build();
		if (file.getName().endsWith(".yaml") || file.getName().endsWith(".yml")) return YAMLConfigurationLoader.builder().setFile(file).build();
		else return HoconConfigurationLoader.builder().setFile(file).build();
	}
	
}
