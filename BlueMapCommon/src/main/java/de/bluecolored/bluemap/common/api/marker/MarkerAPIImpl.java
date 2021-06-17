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
package de.bluecolored.bluemap.common.api.marker;

import de.bluecolored.bluemap.api.marker.MarkerAPI;
import de.bluecolored.bluemap.api.marker.MarkerSet;
import de.bluecolored.bluemap.common.api.BlueMapAPIImpl;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.util.FileUtils;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MarkerAPIImpl implements MarkerAPI {

	private BlueMapAPIImpl api;
	private File markerFile;
	private Map<String, MarkerSetImpl> markerSets;
	private Set<String> removedMarkerSets;
	
	public MarkerAPIImpl(BlueMapAPIImpl api, File markerFile) throws IOException {
		this.api = api;
		this.markerFile = markerFile;
		
		this.markerSets = new ConcurrentHashMap<>();
		this.removedMarkerSets = Collections.newSetFromMap(new ConcurrentHashMap<>());
		
		load();
	}
	
	@Override
	public Collection<MarkerSet> getMarkerSets() {
		return Collections.unmodifiableCollection(this.markerSets.values());
	}

	@Override
	public Optional<MarkerSet> getMarkerSet(String id) {
		return Optional.ofNullable(this.markerSets.get(id));
	}

	@Override
	public synchronized MarkerSet createMarkerSet(String id) {
		MarkerSetImpl set = this.markerSets.get(id);
		
		if (set == null) {
			set = new MarkerSetImpl(id);
			this.markerSets.put(id, set);
		}
		
		return set;
	}

	@Override
	public synchronized boolean removeMarkerSet(String id) {
		if (this.markerSets.remove(id) != null) {
			this.removedMarkerSets.add(id);
			return true;
		}
		
		return false;
	}

	@Override
	public synchronized void load() throws IOException {		
		load(true);
	}
	
	private synchronized void load(boolean overwriteChanges) throws IOException {
		synchronized (MarkerAPIImpl.class) {
			Set<String> externallyRemovedSets = new HashSet<>(markerSets.keySet());

			if (markerFile.exists() && markerFile.isFile()) {
				GsonConfigurationLoader loader = GsonConfigurationLoader.builder().file(markerFile).build();
				ConfigurationNode node = loader.load();

				for (ConfigurationNode markerSetNode : node.node("markerSets").childrenList()) {
					String setId = markerSetNode.node("id").getString();
					if (setId == null) {
						Logger.global.logDebug("Marker-API: Failed to load a markerset: No id defined!");
						continue;
					}

					externallyRemovedSets.remove(setId);
					if (!overwriteChanges && removedMarkerSets.contains(setId)) continue;

					MarkerSetImpl set = markerSets.get(setId);

					try {
						if (set == null) {
							set = new MarkerSetImpl(setId);
							set.load(api, markerSetNode, true);
						} else {
							set.load(api, markerSetNode, overwriteChanges);
						}
						markerSets.put(setId, set);
					} catch (MarkerFileFormatException ex) {
						Logger.global.logDebug("Marker-API: Failed to load marker-set '" + setId + ": " + ex);
					}
				}
			}

			if (overwriteChanges) {
				for (String setId : externallyRemovedSets) {
					markerSets.remove(setId);
				}

				removedMarkerSets.clear();
			}
		}
	}

	@Override
	public synchronized void save() throws IOException {
		synchronized (MarkerAPIImpl.class) {
			load(false);

			FileUtils.createFile(markerFile);

			GsonConfigurationLoader loader = GsonConfigurationLoader.builder().file(markerFile).build();
			ConfigurationNode node = loader.createNode();

			for (MarkerSetImpl set : markerSets.values()) {
				set.save(node.node("markerSets").appendListNode());
			}

			loader.save(node);

			removedMarkerSets.clear();
		}
	}
	
}
