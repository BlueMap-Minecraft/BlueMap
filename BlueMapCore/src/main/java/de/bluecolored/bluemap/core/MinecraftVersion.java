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
package de.bluecolored.bluemap.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum MinecraftVersion {

	MC_1_12 (101200, "1.12", "mc1_12", "https://launcher.mojang.com/v1/objects/0f275bc1547d01fa5f56ba34bdc87d981ee12daf/client.jar"),
	MC_1_13 (101300, "1.13", "mc1_13", "https://launcher.mojang.com/v1/objects/30bfe37a8db404db11c7edf02cb5165817afb4d9/client.jar"),
	MC_1_14 (101400, "1.14", "mc1_13", "https://launcher.mojang.com/v1/objects/8c325a0c5bd674dd747d6ebaa4c791fd363ad8a9/client.jar"),
	MC_1_15 (101500, "1.15", "mc1_15", "https://launcher.mojang.com/v1/objects/e3f78cd16f9eb9a52307ed96ebec64241cc5b32d/client.jar"),
	MC_1_16 (101600, "1.16", "mc1_16", "https://launcher.mojang.com/v1/objects/653e97a2d1d76f87653f02242d243cdee48a5144/client.jar");

	private static final Pattern VERSION_REGEX = Pattern.compile("(?:(?<major>\\d+)\\.(?<minor>\\d+))(?:\\.(?<patch>\\d+))?(?:\\-(?:pre|rc)\\d+)?");

	private final int versionOrdinal;
	private final String versionString;
	private final String resourcePrefix;
	private final String clientDownloadUrl;
	
	MinecraftVersion(int versionOrdinal, String versionString, String resourcePrefix, String clientDownloadUrl) {
		this.versionOrdinal = versionOrdinal;
		this.versionString = versionString;
		this.resourcePrefix = resourcePrefix;
		this.clientDownloadUrl = clientDownloadUrl;
	}

	public String getVersionString() {
		return this.versionString;
	}

	public String getResourcePrefix() {
		return this.resourcePrefix;
	}
	
	public String getClientDownloadUrl() {
		return this.clientDownloadUrl;
	}

	public boolean isAtLeast(MinecraftVersion minVersion) {
		return this.versionOrdinal >= minVersion.versionOrdinal;
	}

	public boolean isAtMost(MinecraftVersion maxVersion) {
		return this.versionOrdinal <= maxVersion.versionOrdinal;
	}

	public static MinecraftVersion fromVersionString(String versionString) {
		Matcher matcher = VERSION_REGEX.matcher(versionString);
		if (!matcher.matches()) throw new IllegalArgumentException("Not a valid version string!");
		
		String normalizedVersionString = matcher.group("major") + "." + matcher.group("minor");
		
		for (MinecraftVersion mcv : values()) {
			if (mcv.versionString.equals(normalizedVersionString)) return mcv;
		}
		
		throw new IllegalArgumentException("No matching version found!");
	}

	public static MinecraftVersion getLatest() {
		return MC_1_16;
	}
	
}
