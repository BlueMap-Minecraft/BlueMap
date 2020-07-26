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
package de.bluecolored.bluemap.core.threejs;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class BufferGeometry {

	public Map<String, BufferAttribute> attributes;
	public MaterialGroup[] groups;

	private BufferGeometry() {}
	
	public BufferGeometry(float[] position, float[] normal, float[] color, float[] uv, MaterialGroup[] groups) {
		this.attributes = new HashMap<>();
		addAttribute("position", new BufferAttribute(position, 3));
		addAttribute("normal", new BufferAttribute(normal, 3));
		addAttribute("color", new BufferAttribute(color, 3));
		addAttribute("uv", new BufferAttribute(uv, 2));

		this.groups = groups;
	}

	public void addAttribute(String name, BufferAttribute attribute) {
		this.attributes.put(name, attribute);
	}

	public boolean isValid() {
		int faceCount = getFaceCount();
		
		for (BufferAttribute attribute : attributes.values()) {
			if (attribute.getItemCount() != faceCount) return false;
		}
		
		return true;
	}

	public int getFaceCount() {
		if (attributes.isEmpty()) return 0;
		BufferAttribute attribute = attributes.values().iterator().next();
		return attribute.getItemCount();
	}

	public String toJson() {
		try {

			StringWriter sw = new StringWriter();
			Gson gson = new GsonBuilder().create();
			JsonWriter json = gson.newJsonWriter(sw);

			json.beginObject(); // main-object

			// set special values
			json.name("type").value("BufferGeometry");
			json.name("uuid").value(UUID.randomUUID().toString().toUpperCase());

			json.name("data").beginObject(); // data
			json.name("attributes").beginObject(); // attributes

			for (Entry<String, BufferAttribute> entry : attributes.entrySet()) {
				json.name(entry.getKey());
				entry.getValue().writeJson(json);
			}

			json.endObject(); // attributes

			json.name("groups").beginArray(); // groups

			// write groups into json
			for (MaterialGroup g : groups) {
				json.beginObject();

				json.name("materialIndex").value(g.getMaterialIndex());
				json.name("start").value(g.getStart());
				json.name("count").value(g.getCount());

				json.endObject();
			}

			json.endArray(); // groups
			json.endObject(); // data
			json.endObject(); // main-object

			// save and return
			json.flush();
			return sw.toString();

		} catch (IOException e) {
			// since we are using a StringWriter there should never be an IO exception
			// thrown
			throw new RuntimeException(e);
		}
	}

	public byte[] toBinary() {
		byte[][] encodedAttrs = new byte[this.attributes.size()][];

		int[] groupArray = new int[this.groups.length * 3];
		int i = 0;
		for (MaterialGroup g : this.groups) {
			groupArray[i++] = g.getMaterialIndex();
			groupArray[i++] = g.getStart();
			groupArray[i++] = g.getCount();
		}
		ByteBuffer buffer = ByteBuffer.allocate(4 * groupArray.length);
		IntBuffer intBuffer = buffer.asIntBuffer();
		intBuffer.put(groupArray);
		byte[] encodedGroups = buffer.array();

		int[] header = new int[this.attributes.size() * 5 + 2];
		int current = 12 + header.length * 4;
		header[0] = current;
		header[1] = encodedGroups.length;
		current +=  encodedGroups.length;
		int j = 2;
		for (Entry<String, BufferAttribute> entry : this.attributes.entrySet()) {
			header[j ++] = current;
			byte[] data = entry.getValue().toBinary();
			header[j ++] = data.length;
			current += data.length;
			int type = AttributeType.valueOf(entry.getKey()).ordinal();
			header[j ++] = type;
			encodedAttrs[type] = data;
			header[j ++] = entry.getValue().isNormalized()?1:0;
			header[j ++] = entry.getValue().getItemSize();
		}

		buffer = ByteBuffer.allocate(4 * header.length);
		intBuffer = buffer.asIntBuffer();
		intBuffer.put(header);
		byte[] encodedHeader = buffer.array();

		int size = 12 + encodedHeader.length + encodedGroups.length;
		for (byte[] attr : encodedAttrs) {
			size += attr.length;
		}

		buffer = ByteBuffer.allocate(8);
		buffer.putInt(size);
		buffer.putInt(header.length);
		byte[] inf = buffer.array();

		byte[] result = new byte[size];
		result[0] = 'B';
		result[1] = 'L';
		result[2] = 'U';
		result[3] = 'E';
		System.arraycopy(inf, 0, result, 4, 8);
		System.arraycopy(encodedHeader, 0, result, 12, encodedHeader.length);
		System.arraycopy(encodedGroups, 0, result, 12 + encodedHeader.length, encodedGroups.length);
		for (int k = 2; k < header.length; k += 5) {
			System.arraycopy(encodedAttrs[header[k+2]], 0, result, header[k], header[k+1]);
		}
		return result;
	}

	public static BufferGeometry fromBinary(byte[] data) throws IOException {
		ByteBuffer buffer = ByteBuffer.wrap(data);
		buffer.order(ByteOrder.BIG_ENDIAN);
		if (data[0] != 'B' || data[1] != 'L' || data[2] != 'U' || data[3] != 'E') {
			throw new IOException("Unrecognized file type: " + data[0] + "," + data[1] + "," + data[2] + "," + data[3]);
		}
		if (buffer.getInt(4) != data.length) {
			throw new IOException("Incomplete data");
		}
		Map<String, BufferAttribute> attributes = new HashMap<>();
		List<MaterialGroup> groups = new ArrayList<>(10);
		int headersize = buffer.getInt(8) * 4;
		int gstart = buffer.getInt(12);
		int glength = buffer.getInt(16);
		for (int i = 20; i < headersize; i += 20) {
			int astart = buffer.getInt(i);
			int alength = buffer.getInt(i+4);
			String type = AttributeType.values()[buffer.getInt(i+8)].name();
			boolean normalized = buffer.getInt(i+12) != 0;
			int itemsize = buffer.getInt(i+16);
			ByteBuffer floatBuffer = ByteBuffer.wrap(data, astart, alength);
			floatBuffer.order(ByteOrder.LITTLE_ENDIAN);
			FloatBuffer fb = floatBuffer.asFloatBuffer();
			float[] floatarray = new float[fb.limit()];
			fb.get(floatarray);
			BufferAttribute bufferAttribute = new BufferAttribute(floatarray, itemsize, normalized);
			attributes.put(type, bufferAttribute);
		}
		for (int i = gstart; i < gstart + glength; i+=12) {
			int materialIndex = buffer.getInt(i);
			int start = buffer.getInt(i+4);
			int count = buffer.getInt(i+8);
			groups.add(new MaterialGroup(materialIndex, start, count));
		}
		BufferGeometry bufferGeometry = new BufferGeometry();
		bufferGeometry.attributes = attributes;
		bufferGeometry.groups = groups.toArray(new MaterialGroup[groups.size()]);
		
		return bufferGeometry;
	}

	public static BufferGeometry fromJson(String jsonString) throws IOException {

		Gson gson = new GsonBuilder().create();
		JsonReader json = gson.newJsonReader(new StringReader(jsonString));

		List<MaterialGroup> groups = new ArrayList<>(10);
		Map<String, BufferAttribute> attributes = new HashMap<>();

		json.beginObject(); // root
		while (json.hasNext()) {
			String name1 = json.nextName();

			if (name1.equals("data")) {
				json.beginObject(); // data
				while (json.hasNext()) {
					String name2 = json.nextName();

					if (name2.equals("attributes")) {
						json.beginObject(); // attributes
						while (json.hasNext()) {
							String name3 = json.nextName();
							attributes.put(name3, BufferAttribute.readJson(json));
						}
						json.endObject(); // attributes
					}

					else if (name2.equals("groups")) {
						json.beginArray(); // groups
						while (json.hasNext()) {
							MaterialGroup group = new MaterialGroup(0, 0, 0);
							json.beginObject(); // group
							while (json.hasNext()) {
								String name3 = json.nextName();

								if (name3.equals("materialIndex")) {
									group.setMaterialIndex(json.nextInt());
								}

								else if (name3.equals("start")) {
									group.setStart(json.nextInt());
								}

								else if (name3.equals("count")) {
									group.setCount(json.nextInt());
								}

								else json.skipValue();
							}
							json.endObject(); // group
							groups.add(group);
						}
						json.endArray(); // groups
					}

					else json.skipValue();
				}
				json.endObject();// data
			}

			else json.skipValue();
		}
		json.endObject(); // root

		groups.sort((g1, g2) -> (int) Math.signum(g1.getStart() - g2.getStart()));
		int nextGroup = 0;
		for (MaterialGroup g : groups) {
			if (g.getStart() != nextGroup)
				throw new IllegalArgumentException("Group did not start at correct index! (Got " + g.getStart() + " but expected " + nextGroup + ")");
			if (g.getCount() < 0) throw new IllegalArgumentException("Group has a negative count! (" + g.getCount() + ")");
			nextGroup += g.getCount();
		}

		BufferGeometry bufferGeometry = new BufferGeometry();
		bufferGeometry.attributes = attributes;
		bufferGeometry.groups = groups.toArray(new MaterialGroup[groups.size()]);
		
		return bufferGeometry;
	}

	public static enum AttributeType{
		position,
		normal,
		color,
		uv,
		ao,
		blocklight,
		sunlight
	}

}
