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
package de.bluecolored.bluemap.core.resourcepack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.world.BlockState;

public class ResourcePack {
	
	public static final String MINECRAFT_CLIENT_URL = "https://launcher.mojang.com/v1/objects/8c325a0c5bd674dd747d6ebaa4c791fd363ad8a9/client.jar";
	
	private Map<Path, Resource> resources;
	
	private TextureProvider textureProvider;
	private BlockColorProvider blockColorProvider;
	private Cache<BlockState, BlockStateResource> blockStateResourceCache;
	
	public ResourcePack(List<File> dataSources, File textureExportFile) throws IOException, NoSuchResourceException {
		this.resources = new HashMap<>();
		
		load(dataSources);
		
		blockStateResourceCache = CacheBuilder.newBuilder()
				.maximumSize(10000)
				.build();
		
		textureProvider = new TextureProvider();
		if (textureExportFile.exists()){
			textureProvider.load(textureExportFile);
		} else {
			textureProvider.generate(this);
			textureProvider.save(textureExportFile);
		}
		
		blockColorProvider = new BlockColorProvider(this);
	}
	
	private void load(List<File> dataSources) throws IOException {
		resources.clear();
		
		//load resourcepacks in order
		for (File resourcePath : dataSources) overrideResourcesWith(resourcePath);
	}
	
	private void overrideResourcesWith(File resourcePath){
		if (resourcePath.isFile() && resourcePath.getName().endsWith(".zip") || resourcePath.getName().endsWith(".jar")){
			overrideResourcesWithZipFile(resourcePath);
			return;
		}
		
		overrideResourcesWith(resourcePath, Paths.get(""));
	}
	
	private void overrideResourcesWith(File resource, Path resourcePath){
		if (resource.isDirectory()){
			for (File childFile : resource.listFiles()){
				overrideResourcesWith(childFile, resourcePath.resolve(childFile.getName()));
			}
			return;
		}
		
		if (resource.isFile()){
			try {
				byte[] bytes = Files.readAllBytes(resource.toPath());
				resources.put(resourcePath, new Resource(bytes));
			} catch (IOException e) {
				Logger.global.logError("Failed to load resource: " + resource, e);
			}
		}
	}
	
	private void overrideResourcesWithZipFile(File resourceFile){
		try (
			ZipFile zipFile = new ZipFile(resourceFile);
		){
			Enumeration<? extends ZipEntry> files = zipFile.entries();
			byte[] buffer = new byte[1024];
			while (files.hasMoreElements()){
				ZipEntry file = files.nextElement();
				if (file.isDirectory()) continue;
				
				Path resourcePath = Paths.get("", file.getName().split("/"));
				if (
						!resourcePath.startsWith(Paths.get("assets", "minecraft", "blockstates")) &&
						!resourcePath.startsWith(Paths.get("assets", "minecraft", "models", "block")) &&
						!resourcePath.startsWith(Paths.get("assets", "minecraft", "textures", "block")) &&
						!resourcePath.startsWith(Paths.get("assets", "minecraft", "textures", "colormap"))
				) continue;
				
				InputStream fileInputStream = zipFile.getInputStream(file);
				
				ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(8, (int) file.getSize()));
				int bytesRead;
				while ((bytesRead = fileInputStream.read(buffer)) != -1){
					bos.write(buffer, 0, bytesRead);
				}

				resources.put(resourcePath, new Resource(bos.toByteArray()));
			}
		} catch (IOException e) {
			Logger.global.logError("Failed to load resource: " + resourceFile, e);
		}
	}
	
	public BlockStateResource getBlockStateResource(BlockState block) throws NoSuchResourceException, InvalidResourceDeclarationException {
		BlockStateResource bsr = blockStateResourceCache.getIfPresent(block);
		
		if (bsr == null){
			bsr = new BlockStateResource(block, this);
			blockStateResourceCache.put(block, bsr);
		}
		
		return bsr;
	}
	
	public TextureProvider getTextureProvider(){
		return textureProvider;
	}
	
	public BlockColorProvider getBlockColorProvider(){
		return blockColorProvider;
	}

	public Map<Path, Resource> getAllResources() {
		return Collections.unmodifiableMap(resources);
	}
	
	public InputStream getResource(Path resourcePath) throws NoSuchResourceException {
		Resource resource = resources.get(resourcePath);
		if (resource == null) throw new NoSuchResourceException("There is no resource with that path: " + resourcePath);
		return resource.getStream();
	}
	
	public class Resource {
		
		private byte[] data;
		
		public Resource(byte[] data) {
			this.data = data;
		}
		
		public InputStream getStream(){
			return new ByteArrayInputStream(data);
		}
		
	}
	
	public static void downloadDefaultResource(File file) throws IOException {
		if (file.exists()) file.delete();
		file.getParentFile().mkdirs();
		FileUtils.copyURLToFile(new URL(MINECRAFT_CLIENT_URL), file, 10000, 10000);
	}
	
}
