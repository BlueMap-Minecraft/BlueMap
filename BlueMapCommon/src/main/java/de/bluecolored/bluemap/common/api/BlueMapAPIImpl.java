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
package de.bluecolored.bluemap.common.api;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.common.MapType;
import de.bluecolored.bluemap.common.api.marker.MarkerAPIImpl;
import de.bluecolored.bluemap.common.api.render.RenderAPIImpl;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.world.World;

public class BlueMapAPIImpl extends BlueMapAPI {

	private static final String IMAGE_ROOT_PATH = "images";
	
	public Plugin blueMap;
	public RenderAPIImpl renderer;
	
	public Map<UUID, BlueMapWorldImpl> worlds;
	public Map<String, BlueMapMapImpl> maps;
	
	public BlueMapAPIImpl(Plugin blueMap) {
		this.blueMap = blueMap;
		
		this.renderer = new RenderAPIImpl(this, blueMap.getRenderManager());
		
		worlds = new HashMap<>();
		for (World world : blueMap.getWorlds()) {
			BlueMapWorldImpl w = new BlueMapWorldImpl(this, world);
			worlds.put(w.getUuid(), w);
		}
		
		maps = new HashMap<>();
		for (MapType map : blueMap.getMapTypes()) {
			BlueMapMapImpl m = new BlueMapMapImpl(this, map);
			maps.put(m.getId(), m);
		}
	}
	
	@Override
	public RenderAPIImpl getRenderAPI() {
		return renderer;
	}

	@Override
	public MarkerAPIImpl getMarkerAPI() throws IOException {
		return new MarkerAPIImpl(this, blueMap.getMainConfig().getWebDataPath().resolve("markers.json").toFile());
	}

	@Override
	public Collection<BlueMapMap> getMaps() {
		return Collections.unmodifiableCollection(maps.values());
	}

	@Override
	public Collection<BlueMapWorld> getWorlds() {
		return Collections.unmodifiableCollection(worlds.values());
	}

	@Override
	public String createImage(BufferedImage image, String path) throws IOException {
		path = path.replaceAll("[^a-zA-Z_\\.\\-\\/]", "_");
		String separator = FileSystems.getDefault().getSeparator();
		
		Path webRoot = blueMap.getMainConfig().getWebRoot().toAbsolutePath();
		Path webDataRoot = blueMap.getMainConfig().getWebDataPath().toAbsolutePath();
		
		Path imagePath;
		if (webDataRoot.startsWith(webRoot)) {
			imagePath = webDataRoot.resolve(Paths.get(IMAGE_ROOT_PATH, path.replace("/", separator) + ".png")).toAbsolutePath();
		} else {
			imagePath = webRoot.resolve("assets").resolve(Paths.get(IMAGE_ROOT_PATH, path.replace("/", separator) + ".png")).toAbsolutePath();
		}

		File imageFile = imagePath.toFile();
		imageFile.getParentFile().mkdirs();
		imageFile.delete();
		imageFile.createNewFile();
		
		if (!ImageIO.write(image, "png", imagePath.toFile()))
			throw new IOException("The format 'png' is not supported!");
		
		return webRoot.relativize(imagePath).toString().replace(separator, "/");
	}

	@Override
	public String getBlueMapVersion() {
		return BlueMap.VERSION;
	}
	
	@Override
	public Optional<BlueMapWorld> getWorld(UUID uuid) {
		return Optional.ofNullable(worlds.get(uuid));
	}

	@Override
	public Optional<BlueMapMap> getMap(String id) {
		return Optional.ofNullable(maps.get(id));
	}
	
	public BlueMapWorldImpl getWorldForUuid(UUID uuid) {
		BlueMapWorldImpl world = worlds.get(uuid);
		
		if (world == null) throw new IllegalArgumentException("There is no world loaded with this UUID: " + uuid.toString());
		
		return world;
	}
	
	public BlueMapMapImpl getMapForId(String mapId) {
		BlueMapMapImpl map = maps.get(mapId);

		if (map == null) throw new IllegalArgumentException("There is no map loaded with this id: " + mapId);
		
		return map;
	}
	
	public void register() {
		try {
			BlueMapAPI.registerInstance(this);
		} catch (ExecutionException ex) {
			Logger.global.logError("BlueMapAPI: A BlueMapAPIListener threw an exception (onEnable)!", ex.getCause());
		}
	}
	
	public void unregister() {
		try {
			BlueMapAPI.unregisterInstance(this);
		} catch (ExecutionException ex) {
			Logger.global.logError("BlueMapAPI: A BlueMapAPIListener threw an exception (onDisable)!", ex.getCause());
		}
	}

}
