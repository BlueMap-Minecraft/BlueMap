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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import com.flowpowered.math.vector.Vector3d;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapAPIListener;
import de.bluecolored.bluemap.api.marker.MarkerAPI;
import de.bluecolored.bluemap.api.marker.MarkerSet;
import de.bluecolored.bluemap.api.marker.POIMarker;
import de.bluecolored.bluemap.api.marker.Shape;

public class BlueMapIntegration implements BlueMapAPIListener {

	@Override
	public void onEnable(BlueMapAPI blueMapApi) {
		
		BufferedImage testIcon = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
		Graphics g = testIcon.getGraphics();
		g.setColor(Color.GREEN);
		g.fillOval(10, 15, 5, 10);
		
		String icon = "";
		try {
			icon = blueMapApi.createImage(testIcon, "test/icon");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			
			MarkerAPI markerApi = blueMapApi.getMarkerAPI();
			
			MarkerSet set = markerApi.createMarkerSet("testmarker");
			set.setLabel("Testmarker");
			set.setDefaultHidden(true);
			
			markerApi.save();
			markerApi.load();
			
			set.createShapeMarker("shape1", blueMapApi.getMap("world").get(), Vector3d.from(0, 70.023487, -5.23432542), Shape.createCircle(0, -5,  100, 20), 70);

			MarkerAPI markerApi2 = blueMapApi.getMarkerAPI();
			
			markerApi.save();
			
			markerApi2.createMarkerSet("testmarker").createPOIMarker("poi1", blueMapApi.getMap("world").get(), Vector3d.from(10, 70.023487, -5.234322));
			markerApi2.save();
			
			markerApi.load();
			((POIMarker) markerApi.getMarkerSet("testmarker").get().getMarker("poi1").get()).setIcon(icon, 10, 15);
			
			markerApi.save();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
}
