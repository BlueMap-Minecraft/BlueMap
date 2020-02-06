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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.flowpowered.math.GenericMath;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class BufferGeometry {

	public final float[] position, normal, color, uv;
	public final MaterialGroup[] groups;
	
	public BufferGeometry(float[] position, float[] normal, float[] color, float[] uv, MaterialGroup[] groups) {
		this.position = position;
		this.normal = normal;
		this.color = color;
		this.uv = uv;
		this.groups = groups;
	}
	
	public int getFaceCount(){
		return Math.floorDiv(position.length, 3);
	}
	
	public String toJson() {
		try {
		
			StringWriter sw = new StringWriter();
			Gson gson = new GsonBuilder().create();
			JsonWriter json = gson.newJsonWriter(sw);
			
			json.beginObject();
			
			//set special values
			json.name("type").value("BufferGeometry");
			json.name("uuid").value(UUID.randomUUID().toString().toUpperCase());
			
			json.name("data").beginObject();
			json.name("attributes").beginObject();
			
			json.name("position");
			floatArray2Json(json, position, 3, false);
			
			json.name("normal");
			floatArray2Json(json, normal, 3, true);
			
			json.name("color");
			floatArray2Json(json, color, 3, false);
			
			json.name("uv");
			floatArray2Json(json, uv, 2, false);
			
			json.endObject(); //attributes
			
	
			json.name("groups").beginArray();
			
			//write groups into json
			for (BufferGeometry.MaterialGroup g : groups){
				json.beginObject();
				
				json.name("materialIndex").value(g.getMaterialIndex());
				json.name("start").value(g.getStart());
				json.name("count").value(g.getCount());
	
				json.endObject();
			}
	
			json.endArray(); //groups
			json.endObject(); //data
			json.endObject(); //main-object
			
			//save and return
			json.flush();
			return sw.toString();
		
		} catch (IOException e){
			//since we are using a StringWriter there should never be an IO exception thrown
			throw new RuntimeException(e);
		}
	}
	
	public static BufferGeometry fromJson(String jsonString) throws IOException {

		Gson gson = new GsonBuilder().create();
		JsonReader json = gson.newJsonReader(new StringReader(jsonString));
		
		List<Float> positionList = new ArrayList<>(300);
		List<Float> normalList = new ArrayList<>(300);
		List<Float> colorList = new ArrayList<>(300);
		List<Float> uvList = new ArrayList<>(200);
		List<MaterialGroup> groups = new ArrayList<>(10);
		
		json.beginObject(); //root
		while (json.hasNext()){
			String name1 = json.nextName();
			
			if(name1.equals("data")){
				json.beginObject(); //data
				while (json.hasNext()){
					String name2 = json.nextName();
					
					if(name2.equals("attributes")){
						json.beginObject(); //attributes
						while (json.hasNext()){
							String name3 = json.nextName();
							
							if(name3.equals("position")){
								json2FloatList(json, positionList);
							}
							
							else if(name3.equals("normal")){
								json2FloatList(json, normalList);
							}
							
							else if(name3.equals("color")){
								json2FloatList(json, colorList);
							}
							
							else if(name3.equals("uv")){
								json2FloatList(json, uvList);
							}
							
							else json.skipValue();
						}
						json.endObject(); //attributes
					}
					
					else if (name2.equals("groups")){
						json.beginArray(); //groups
						while (json.hasNext()){
							MaterialGroup group = new MaterialGroup(0, 0, 0);
							json.beginObject(); //group
							while (json.hasNext()){
								String name3 = json.nextName();
								
								if(name3.equals("materialIndex")){
									group.setMaterialIndex(json.nextInt());
								}
								
								else if(name3.equals("start")){
									group.setStart(json.nextInt());
								}
								
								else if(name3.equals("count")){
									group.setCount(json.nextInt());
								}
								
								else json.skipValue();
							}
							json.endObject(); //group
							groups.add(group);
						}
						json.endArray(); //groups
					}
					
					else json.skipValue();
				}
				json.endObject();//data
			}
			
			else json.skipValue();
		}
		json.endObject(); //root	
		
		//check if this is a valid BufferGeometry
		int faceCount = Math.floorDiv(positionList.size(), 3);
		if (positionList.size() != faceCount * 3) throw new IllegalArgumentException("Wrong count of positions! (Got " + positionList.size() + " but expected " + (faceCount * 3) + ")");
		if (normalList.size() != faceCount * 3) throw new IllegalArgumentException("Wrong count of normals! (Got " + normalList.size() + " but expected " + (faceCount * 3) + ")");
		if (colorList.size() != faceCount * 3) throw new IllegalArgumentException("Wrong count of colors! (Got " + colorList.size() + " but expected " + (faceCount * 3) + ")");
		if (uvList.size() != faceCount * 2) throw new IllegalArgumentException("Wrong count of uvs! (Got " + uvList.size() + " but expected " + (faceCount * 2) + ")");
		
		groups.sort((g1, g2) -> (int) Math.signum(g1.getStart() - g2.getStart()));
		int nextGroup = 0;
		for (MaterialGroup g : groups){
			if(g.getStart() != nextGroup) throw new IllegalArgumentException("Group did not start at correct index! (Got " + g.getStart() + " but expected " + nextGroup + ")");
			if(g.getCount() < 0) throw new IllegalArgumentException("Group has a negative count! (" + g.getCount() + ")");
			nextGroup += g.getCount();
		}
		
		//collect values in arrays
		float[] position = new float[positionList.size()];
		for (int i = 0; i < position.length; i++) {
			position[i] = positionList.get(i);
		}
		
		float[] normal = new float[normalList.size()];
		for (int i = 0; i < normal.length; i++) {
			normal[i] = normalList.get(i);
		}
		
		float[] color = new float[colorList.size()];
		for (int i = 0; i < color.length; i++) {
			color[i] = colorList.get(i);
		}
		
		float[] uv = new float[uvList.size()];
		for (int i = 0; i < uv.length; i++) {
			uv[i] = uvList.get(i);
		}
		
		return new BufferGeometry(position, normal, color, uv, 
				groups.toArray(new MaterialGroup[groups.size()])
			);
	}
	
	private static void json2FloatList(JsonReader json, List<Float> list) throws IOException {
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
			
			else json.skipValue();
		}
		json.endObject(); //root
	}
	
	private static void floatArray2Json(JsonWriter json, float[] array, int itemSize, boolean normalized) throws IOException {
		json.beginObject();

		json.name("type").value("Float32Array");
		json.name("itemSize").value(itemSize);
		json.name("normalized").value(normalized);
		
		json.name("array").beginArray();
		for (int i = 0; i < array.length; i++){
			//rounding and remove ".0" to save string space
			double d = GenericMath.round(array[i], 4);
			if (d == (int) d) json.value((int) d);
			else json.value(d);
		}
		json.endArray();
		
		json.endObject();
	}
	
	public static class MaterialGroup {
		private int materialIndex;
		private int start;
		private int count;
		
		public MaterialGroup(int materialIndex, int start, int count) {
			this.materialIndex = materialIndex;
			this.start = start;
			this.count = count;
		}

		public int getMaterialIndex() {
			return materialIndex;
		}

		public int getStart() {
			return start;
		}

		public int getCount() {
			return count;
		}

		public void setMaterialIndex(int materialIndex) {
			this.materialIndex = materialIndex;
		}

		public void setStart(int start) {
			this.start = start;
		}

		public void setCount(int count) {
			this.count = count;
		}
	}
	
}
