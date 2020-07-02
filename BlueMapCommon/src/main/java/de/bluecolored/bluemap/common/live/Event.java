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
package de.bluecolored.bluemap.common.live;

import java.io.IOException;

import com.google.gson.stream.JsonWriter;

public class Event {

	private String type;
	private long time;
	private long expires;
	private String jsonData;
	
	public Event(String type, long time, long expires, String jsonData) {
		this.type = type;
		this.time = time;
		this.expires = expires;
		this.jsonData = jsonData;
	}
	
	public void serialize(JsonWriter writer) throws IOException {
		writer.beginObject();
		
		writer.name("type").value(type);
		writer.name("time").value(time);
		writer.name("jsonData").jsonValue(jsonData);
		
		writer.endObject();
	}

	public String getType() {
		return type;
	}

	public long getTime() {
		return time;
	}

	public long getExpireTime() {
		return expires;
	}

	public String getJsonData() {
		return jsonData;
	}
	
	
	
}
