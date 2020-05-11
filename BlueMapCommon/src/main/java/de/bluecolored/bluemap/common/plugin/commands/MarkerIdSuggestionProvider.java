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

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.marker.Marker;
import de.bluecolored.bluemap.api.marker.MarkerAPI;
import de.bluecolored.bluemap.api.marker.MarkerSet;
import de.bluecolored.bluemap.api.marker.POIMarker;
import de.bluecolored.bluemap.core.logger.Logger;

public class MarkerIdSuggestionProvider<S> extends AbstractSuggestionProvider<S> {

	private static MarkerIdSuggestionProvider<?> instance;
	
	private MarkerAPI markerApi;
	private long lastUpdate = -1;
	
	private MarkerIdSuggestionProvider() {}
	
	@Override
	public Collection<String> getPossibleValues() {
		Collection<String> values = new HashSet<>();
		
		if (markerApi == null || lastUpdate + 1000 * 60 < System.currentTimeMillis()) { // only (re)load marker-values max every minute
			lastUpdate = System.currentTimeMillis();
			
			Optional<BlueMapAPI> api = BlueMapAPI.getInstance();
			if (!api.isPresent()) return values;
			
			try {
				markerApi = api.get().getMarkerAPI();
			} catch (IOException e) {
				Logger.global.noFloodError("0FEz5tm345rf", "Failed to load MarkerAPI!", e);
				return values;
			}
		}
		
		MarkerSet set = markerApi.getMarkerSet(Commands.DEFAULT_MARKER_SET_ID).orElse(null);
		if (set != null) {
			for (Marker marker : set.getMarkers()) {
				if (marker instanceof POIMarker) {
					values.add(marker.getId());
				}
			}
		}
		
		return values;
	}
	
	public void forceUpdate() {
		lastUpdate = -1;
	}
	
	@SuppressWarnings("unchecked")
	public static <S> MarkerIdSuggestionProvider<S> getInstance(){
		if (instance == null) {
			instance = new MarkerIdSuggestionProvider<>();
		}
		
		return (MarkerIdSuggestionProvider<S>) instance;
	}
	
}
