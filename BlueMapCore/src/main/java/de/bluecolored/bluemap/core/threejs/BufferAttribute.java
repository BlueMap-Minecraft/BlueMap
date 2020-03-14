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
/**
 * 
 */
package de.bluecolored.bluemap.core.threejs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Represents a ThreeJS BufferAttribute
 */
public class BufferAttribute {

	private int itemSize;
	private boolean normalized;
	private float[] values;

	/**
	 * Creates a new {@link BufferAttribute} with the defined item-size
	 */
	public BufferAttribute(float[] values, int itemSize) {
		Preconditions.checkArgument(values.length % itemSize == 0, "The length of the values-array is not a multiple of the item-size!");

		this.values = values;
		this.itemSize = itemSize;
		this.normalized = false;
	}

	/**
	 * Creates a new {@link BufferAttribute} with the defined item-size and the
	 * defined threejs "normalized" attribute
	 */
	public BufferAttribute(float[] values, int itemSize, boolean normalized) {
		Preconditions.checkArgument(values.length % itemSize == 0, "The length of the values-array is not a multiple of the item-size!");

		this.values = values;
		this.itemSize = itemSize;
		this.normalized = normalized;
	}

	public void writeJson(JsonWriter json) throws IOException {
		json.beginObject();

		json.name("type").value("Float32Array");
		json.name("itemSize").value(itemSize);
		json.name("normalized").value(normalized);

		json.name("array").beginArray();
		for (int i = 0; i < values.length; i++) {
			// rounding and remove ".0" to save string space
			float d = Math.round(values[i] * 10000f) / 10000f;
			if (d == (int) d) json.value((int) d);
			else json.value(d);
		}
		json.endArray();

		json.endObject();
	}

	public int getItemSize() {
		return this.itemSize;
	}

	public boolean isNormalized() {
		return this.normalized;
	}
	
	public int getValueCount() {
		return this.values.length;
	}
	
	public int getItemCount() {
		return Math.floorDiv(getValueCount(), getItemSize());
	}
	
	public float[] values() {
		return values;
	}
	
	public static BufferAttribute readJson(JsonReader json) throws IOException {
		List<Float> list = new ArrayList<>(1000);
		int itemSize = 1;
		boolean normalized = false;
		
		json.beginObject(); //root
		while (json.hasNext()){
			String name = json.nextName();
			
			if(name.equals("array")){
				json.beginArray(); //array
				while (json.hasNext()){
					list.add(new Float(json.nextDouble()));
				}
				json.endArray(); //array
			}
			
			else if (name.equals("itemSize")) {
				itemSize = json.nextInt();
			}
			
			else if (name.equals("normalized")) {
				normalized = json.nextBoolean();
			}
			
			else json.skipValue();
		}
		json.endObject(); //root
		
		//collect values in array
		float[] values = new float[list.size()];
		for (int i = 0; i < values.length; i++) {
			values[i] = list.get(i);
		}
		
		return new BufferAttribute(values, itemSize, normalized);
	}

}
