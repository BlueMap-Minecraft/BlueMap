package de.bluecolored.bluemap.core;

import java.io.IOException;

import de.bluecolored.bluemap.core.logger.Logger;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;

public class BlueMap {

	public static final String VERSION;
	static {
		String version = "DEV";
		try {
			version = GsonConfigurationLoader.builder().setURL(BlueMap.class.getResource("/core.json")).build().load().getNode("version").getString("DEV");
		} catch (IOException ex) {
			Logger.global.logError("Failed to load core.json from resources!", ex);
		}
		
		if (version.equals("${version}")) version = "DEV";
		
		VERSION = version;
	}

}
