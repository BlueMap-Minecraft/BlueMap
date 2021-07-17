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

import com.google.gson.*;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resourcepack.fileaccess.FileAccess;
import de.bluecolored.bluemap.core.util.FileUtils;
import de.bluecolored.bluemap.core.util.math.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

/**
 * A {@link TextureGallery} is managing {@link Texture}s and their id's and path's.<br>
 * I can also load and generate the texture.json file, or load new {@link Texture}s from a {@link FileAccess}. 
 */
public class TextureGallery {

	private static final String EMPTY_BASE64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAQAAAC1+jfqAAAAEUlEQVR42mNkIAAYRxWMJAUAE5gAEdz4t9QAAAAASUVORK5CYII=";
	
	private Map<String, Texture> textureMap;
	private List<Texture> textureList;
	
	public TextureGallery() {
		textureMap = new HashMap<>();
		textureList = new ArrayList<>();
	}
	
	/**
	 * Returns a {@link Texture} by its id, there can always be only one texture per id in a gallery.
	 * @param id The texture id
	 * @return The {@link Texture}
	 */
	public Texture get(int id) {
		return textureList.get(id);
	}
	
	/**
	 * Returns a {@link Texture} by its path, there can always be only one texture per path in a gallery.
	 * @param path The texture-path
	 * @return The {@link Texture}
	 */
	public Texture get(String path) {
		Texture texture = textureMap.get(path);
		if (texture == null) throw new NoSuchElementException("There is no texture with the path " + path + " in this gallery!");
		return texture;
	}
	
	/**
	 * The count of {@link Texture}s managed by this gallery
	 * @return The count of textures
	 */
	public int size() {
		return textureList.size();
	}
	
	/**
	 * Generates a texture.json file with all the {@link Texture}s in this gallery
	 * @param file The file to save the json in
	 * @throws IOException If an IOException occurs while writing
	 */
	public void saveTextureFile(File file) throws IOException {
		
		JsonArray textures = new JsonArray();
		for (int i = 0; i < textureList.size(); i++) {
			Texture texture = textureList.get(i);
			
			JsonObject textureNode = new JsonObject();
			textureNode.addProperty("id", texture.getPath());
			textureNode.addProperty("texture", texture.getTexture());
			textureNode.addProperty("transparent", texture.isHalfTransparent());
			
			Color color = texture.getColorStraight();
			JsonArray colorNode = new JsonArray();
			colorNode.add(color.r);
			colorNode.add(color.g);
			colorNode.add(color.b);
			colorNode.add(color.a);
			
			textureNode.add("color", colorNode);
			
			textures.add(textureNode);
		}
		
		JsonObject root = new JsonObject();
		root.add("textures", textures);
		
		Gson gson = new GsonBuilder()
				.setPrettyPrinting()
				.create();
		String json = gson.toJson(root);
		
		if (file.exists()) FileUtils.delete(file);
		FileUtils.createFile(file);
		
		try (FileWriter fileWriter = new FileWriter(file)) {
			fileWriter.append(json);
			fileWriter.flush();
		}
		
	}
	
	/**
	 * Loads all the {@link Texture}s from the provided texture.json file, removes any existing {@link Texture}s from this gallery.
	 * @param file The texture.json file.
	 * @throws IOException If an IOException occurs while reading the file.
	 * @throws ParseResourceException If the whole file can not be read. Errors with single textures are logged and ignored. 
	 */
	public synchronized void loadTextureFile(File file) throws IOException, ParseResourceException {
		textureList.clear();
		textureMap.clear();
		
		try (FileReader fileReader = new FileReader(file)){
			JsonStreamParser jsonFile = new JsonStreamParser(fileReader);
			JsonArray textures = jsonFile.next().getAsJsonObject().getAsJsonArray("textures");
			int size = textures.size();
			for (int i = 0; i < size; i++) {
				while (i >= textureList.size()) { //prepopulate with placeholder so we don't get an IndexOutOfBounds below
					textureList.add(new Texture(textureList.size(), "empty", new Color(), false, EMPTY_BASE64));
				}
				
				try {
					JsonObject texture = textures.get(i).getAsJsonObject();
					String path = texture.get("id").getAsString();
					boolean transparent = texture.get("transparent").getAsBoolean();
					Color color = readColor(texture.get("color").getAsJsonArray());
					textureList.set(i, new Texture(i, path, color, transparent, EMPTY_BASE64));
				} catch (ParseResourceException | RuntimeException ex) {
					Logger.global.logWarning("Failed to load texture with id " + i + " from texture file " + file + "!");
				}
			}
		} catch (IOException ex) {
			throw ex;
		} catch (RuntimeException ex) {
			throw new ParseResourceException("Invalid texture file format!", ex);
		} finally {
			regenerateMap();
		}
	}
	
	/**
	 * Loads a {@link Texture} from the {@link FileAccess} and the given path and returns it.<br>
	 * If there is already a {@link Texture} with this path in this Gallery it replaces the {@link Texture} with the new one 
	 * and the new one will have the same id as the old one.<br>
	 * Otherwise the {@link Texture} will be added to the end of this gallery with the next available id.
	 * @param fileAccess The {@link FileAccess} to load the image from.
	 * @param path The path of the image on the {@link FileAccess}
	 * @return The loaded {@link Texture}
	 * @throws FileNotFoundException If there is no image in that FileAccess on that path
	 * @throws IOException If an IOException occurred while loading the file
	 */
	public synchronized Texture loadTexture(FileAccess fileAccess, String path) throws FileNotFoundException, IOException {
		try (InputStream input = fileAccess.readFile(path)) {
			BufferedImage image = ImageIO.read(input);
			if (image == null) throw new IOException("Failed to read image: " + path);
			
			//crop off animation frames
			if (image.getHeight() > image.getWidth()){
				image = image.getSubimage(0, 0, image.getWidth(), image.getWidth());
			}
			
			//check halfTransparency
			boolean halfTransparent = checkHalfTransparent(image);
			
			//calculate color
			Color color = calculateColor(image);
			
			//write to Base64
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			ImageIO.write(image, "png", os);
			String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(os.toByteArray());
			
			//replace if texture with this path already exists
			Texture texture = textureMap.get(path);
			if (texture != null) {
				texture = new Texture(texture.getId(), path, color, halfTransparent, base64);
				textureMap.put(path, texture);
				textureList.set(texture.getId(), texture);
			} else {
				texture = new Texture(textureList.size(), path, color, halfTransparent, base64);
				textureMap.put(path, texture);
				textureList.add(texture);
			}
			
			return texture;
		}
	}
	
	/**
	 * Tries to reload all {@link Texture}s from the given {@link FileAccess}<br>
	 * <br>
	 * Exceptions are being logged and ignored. 
	 * @param fileAccess The {@link FileAccess} to load the {@link Texture}s from
	 */
	public synchronized void reloadAllTextures(FileAccess fileAccess) {
		for (Texture texture : textureList.toArray(new Texture[textureList.size()])) {
			try {
				loadTexture(fileAccess, texture.getPath());
			} catch (IOException e) {
				Logger.global.noFloodWarning("TextureGallery-uiz78tef5", "Failed to reload texture: " + texture.getPath());
				Logger.global.noFloodWarning("TextureGallery-89763455h", "This happens if the resource-packs have changed, but you have not deleted your already generated maps. This might result in broken map-models!");
				Logger.global.noFloodWarning("TextureGallery-re56ugb56", "(Future warnings of this will be suppressed, so more textures might have failed to load after this)");
			}
		}
	}

	private synchronized void regenerateMap() {
		textureMap.clear();
		for (int i = 0; i < textureList.size(); i++) {
			Texture texture = textureList.get(i);
			textureMap.put(texture.getPath(), texture);
		}
	}

	private Color readColor(JsonArray jsonArray) throws ParseResourceException {
		if (jsonArray.size() < 4) throw new ParseResourceException("Failed to load Vector4: Not enough values in list-node!");
		
		float r = jsonArray.get(0).getAsFloat();
		float g = jsonArray.get(1).getAsFloat();
		float b = jsonArray.get(2).getAsFloat();
		float a = jsonArray.get(3).getAsFloat();
		
		return new Color().set(r, g, b, a, false);
	}
	
	private boolean checkHalfTransparent(BufferedImage image){
		for (int x = 0; x < image.getWidth(); x++){
			for (int y = 0; y < image.getHeight(); y++){
				int pixel = image.getRGB(x, y);
				int alpha = (pixel >> 24) & 0xff;
				if (alpha > 0x00 && alpha < 0xff){
					return true;
				}
			}
		}
		
		return false;
	}
	
	private Color calculateColor(BufferedImage image){
		float alpha = 0f, red = 0f, green = 0f, blue = 0f;
		int count = 0;
		
		for (int x = 0; x < image.getWidth(); x++){
			for (int y = 0; y < image.getHeight(); y++){
				int pixel = image.getRGB(x, y);
				float pixelAlpha = ((pixel >> 24) & 0xff) / 255f;
				float pixelRed = ((pixel >> 16) & 0xff) / 255f;
				float pixelGreen = ((pixel >> 8) & 0xff) / 255f;
				float pixelBlue = (pixel & 0xff) / 255f;
		        
		        count++;
		        alpha += pixelAlpha;
		        red += pixelRed * pixelAlpha;
		        green += pixelGreen * pixelAlpha;
		        blue += pixelBlue * pixelAlpha;
			}
		}
		
		if (count == 0 || alpha == 0) return new Color();
		
		red /= alpha;
		green /= alpha;
		blue /= alpha;
		alpha /= count;
		
		return new Color().set(red, green, blue, alpha, false);
	}
	
}
