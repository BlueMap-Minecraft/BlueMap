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
package de.bluecolored.bluemap.core.world;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents a BlockState<br>
 * It is important that {@link #hashCode} and {@link #equals} are implemented correctly, for the caching to work properly.<br>
 * <br>
 * <i>The implementation of this class has to be thread-save!</i><br>
 */
public class BlockState {
	
	private static Pattern BLOCKSTATE_SERIALIZATION_PATTERN = Pattern.compile("^(.+?)(?:\\[(.*)\\])?$");
	
	public static final BlockState AIR = new BlockState("minecraft:air", Collections.emptyMap());
	public static final BlockState MISSING = new BlockState("bluemap:missing", Collections.emptyMap());

	private boolean hashed;
	private int hash;

	private final String namespace;
	private final String id;
	private final String fullId;
	private final Map<String, String> properties;

	public BlockState(String id) {
		this(id, Collections.emptyMap());
	}
	
	public BlockState(String id, Map<String, String> properties) {
		this.hashed = false;
		this.hash = 0;
		
		this.properties = Collections.unmodifiableMap(new HashMap<>(properties));
		
		//resolve namespace
		String namespace = "minecraft";
		int namespaceSeperator = id.indexOf(':');
		if (namespaceSeperator > 0) {
			namespace = id.substring(0, namespaceSeperator); 
			id = id.substring(namespaceSeperator + 1);
		}
		
		this.id = id;
		this.namespace = namespace;
		this.fullId = namespace + ":" + id;
	}
	
	private BlockState(BlockState blockState, String withKey, String withValue) {
		this.hashed = false;
		this.hash = 0;
		
		Map<String, String> props = new HashMap<>(blockState.getProperties());
		props.put(withKey, withValue);
		
		this.id = blockState.getId();
		this.namespace = blockState.getNamespace();
		this.fullId = namespace + ":" + id;
		this.properties = Collections.unmodifiableMap(props);
	}

	/**
	 * The namespace of this blockstate,<br>
	 * this is always "minecraft" in vanilla.<br>
	 */
	public String getNamespace() {
		return namespace;
	}
	
	/**
	 * The id of this blockstate,<br>
	 * also the name of the resource-file without the filetype that represents this block-state <i>(found in mineceraft in assets/minecraft/blockstates)</i>.<br>
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Returns the namespaced id of this blockstate
	 */
	public String getFullId() {
		return fullId;
	}
	
	/**
	 * An immutable map of all properties of this block.<br>
	 * <br>
	 * For Example:<br>
	 * <code>
	 * facing = east<br>
	 * half = bottom<br>
	 * </code>
	 */
	public Map<String, String> getProperties() {
		return properties;
	}
	
	/**
	 * Returns a new BlockState with the given property changed
	 */
	public BlockState with(String property, String value) {
		return new BlockState(this, property, value);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		
		if (!(obj instanceof BlockState)) return false;
		BlockState b = (BlockState) obj;
		if (!Objects.equals(getFullId(), b.getFullId())) return false;
		if (!Objects.equals(getProperties(), b.getProperties())) return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		if (!hashed){
			hash = Objects.hash( getFullId(), getProperties() );
			hashed = true;
		}
		
		return hash;
	}
	
	@Override
	public String toString() {
		StringJoiner sj = new StringJoiner(",");
		for (Entry<String, String> e : getProperties().entrySet()){
			sj.add(e.getKey() + "=" + e.getValue());
		}
		
		return getFullId() + "[" + sj.toString() + "]";
	}
	
	public static BlockState fromString(String serializedBlockState) throws IllegalArgumentException {
		try {
			Matcher m = BLOCKSTATE_SERIALIZATION_PATTERN.matcher(serializedBlockState);
			m.find();
	
			Map<String, String> pt = new HashMap<>();
			String g2 = m.group(2);
			if (g2 != null && !g2.isEmpty()){
				String[] propertyStrings = g2.trim().split(",");
				for (String s : propertyStrings){
					String[] kv = s.split("=", 2);
					pt.put(kv[0], kv[1]);
				}
			}
	
			String blockId = m.group(1).trim();
			
			return new BlockState(blockId, pt);
		} catch (Exception ex) {
			throw new IllegalArgumentException("'" + serializedBlockState + "' could not be parsed to a BlockState!");
		}
	}
	
}
