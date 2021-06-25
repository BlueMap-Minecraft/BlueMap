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
package de.bluecolored.bluemap.core.mca.extensions;

import de.bluecolored.bluemap.core.MinecraftVersion;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class WoodenFenceConnectExtension extends ConnectSameOrFullBlockExtension {
	
	private final Set<String> affectedBlockIds;
	
	public WoodenFenceConnectExtension(MinecraftVersion version) {
		if (version.isBefore(MinecraftVersion.THE_FLATTENING)) {
			affectedBlockIds = new HashSet<>(Arrays.asList(
					"minecraft:fence",
					"minecraft:spruce_fence",
					"minecraft:birch_fence",
					"minecraft:jungle_fence",
					"minecraft:dark_oak_fence",
					"minecraft:acacia_fence"
			));
		} else {
			affectedBlockIds = new HashSet<>(Arrays.asList(
					"minecraft:oak_fence",
					"minecraft:spruce_fence",
					"minecraft:birch_fence",
					"minecraft:jungle_fence",
					"minecraft:dark_oak_fence",
					"minecraft:acacia_fence"
			));
		}
	}
	
	@Override
	public Set<String> getAffectedBlockIds() {
		return affectedBlockIds;
	}

}
