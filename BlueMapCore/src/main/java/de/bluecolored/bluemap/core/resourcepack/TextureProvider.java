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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import com.flowpowered.math.vector.Vector4f;

import de.bluecolored.bluemap.core.resourcepack.ResourcePack.Resource;
import de.bluecolored.bluemap.core.util.ConfigUtil;
import de.bluecolored.bluemap.core.util.MathUtil;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;

public class TextureProvider {
	
	private Map<String, Integer> indexMap;
	private List<Texture> textures;
	
	public TextureProvider() throws IOException {
		this.indexMap = new ConcurrentHashMap<>();
		this.textures = new Vector<>();
	}
	
	public int getTextureIndex(String textureId) throws NoSuchTextureException {
		Integer tex = indexMap.get(textureId);
		
		if (tex == null){
			throw new NoSuchTextureException("There is no texture with id: " + textureId);
		}
		
		return tex.intValue();
	}
	
	public Texture getTexture(String textureId) throws NoSuchTextureException {
		return getTexture(getTextureIndex(textureId));
	}
	
	public Texture getTexture(int index){
		return textures.get(index);
	}
	
	public void generate(ResourcePack resources) throws IOException {
		indexMap.clear();
		textures.clear();

		Path textureRoot = Paths.get("assets", "minecraft", "textures");
		for (Entry<Path, Resource> entry : resources.getAllResources().entrySet()){
			if (entry.getKey().startsWith(textureRoot) && entry.getKey().toString().endsWith(".png")){
				BufferedImage image = ImageIO.read(entry.getValue().getStream());
				if (image == null) throw new IOException("Failed to read Image: " + entry.getKey());
				
				String path = textureRoot.relativize(entry.getKey()).normalize().toString();
				String id = path
						.substring(0, path.length() - ".png".length())
						.replace(File.separatorChar, '/');
				
				Texture texture = new Texture(id, image);
				textures.add(texture);
				indexMap.put(id, textures.size() - 1);
			}
		}
	}
	
	public void load(File file) throws IOException {
		
		indexMap.clear();
		textures.clear();
		
		GsonConfigurationLoader loader = GsonConfigurationLoader.builder().setFile(file).build();
		ConfigurationNode node = loader.load();
		
		int i = 0;
		for(ConfigurationNode n : node.getNode("textures").getChildrenList()){
			Texture t = new Texture(
					n.getNode("id").getString(), 
					n.getNode("texture").getString(), 
					n.getNode("transparent").getBoolean(false),
					ConfigUtil.readVector4f(n.getNode("color"))
				);
			
			textures.add(t);
			indexMap.put(t.getId(), i++);
		}
	}
	
	public void save(File file) throws IOException {
		
		if (!file.exists()) {
			file.getParentFile().mkdirs();
			file.createNewFile();
		}
		
		GsonConfigurationLoader loader = GsonConfigurationLoader.builder().setFile(file).build();
		ConfigurationNode node = loader.createEmptyNode();
		
		for (Texture t : textures){
			ConfigurationNode n = node.getNode("textures").getAppendedNode();
			n.getNode("id").setValue(t.getId());
			n.getNode("texture").setValue(t.getBase64());
			n.getNode("transparent").setValue(t.isHalfTransparent());
			ConfigUtil.writeVector4f(n.getNode("color"), t.getColor());
		}
		
		loader.save(node);
	}
	
	public class Texture {
		
		private String id;
		private String base64;
		private boolean halfTransparent;
		private Vector4f color;
		
		public Texture(String id, String base64, boolean halfTransparent, Vector4f color){
			this.id = id;
			this.halfTransparent = halfTransparent;
			this.base64 = base64;
			this.color = color;
		}
		
		public Texture(String id, BufferedImage image) throws IOException {
			this.id = id;
			
			//crop off animation frames
			if (image.getHeight() > image.getWidth()){
				BufferedImage cropped = new BufferedImage(image.getWidth(), image.getWidth(), image.getType());
				Graphics2D g = cropped.createGraphics();
				g.drawImage(image, 0, 0, null);
				image = cropped;
			}
			
			//check halfTransparency
			this.halfTransparent = checkHalfTransparent(image);
			
			//calculate color
			this.color = calculateColor(image);
			
			//write to Base64
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			ImageIO.write(image, "png", os);
			this.base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(os.toByteArray());
		}
		
		private Vector4f calculateColor(BufferedImage image){
			Vector4f color = Vector4f.ZERO;
			
			for (int x = 0; x < image.getWidth(); x++){
				for (int y = 0; y < image.getHeight(); y++){
					int pixel = image.getRGB(x, y);
					double alpha = (double)((pixel >> 24) & 0xff) / (double) 0xff;
			        double red = (double)((pixel >> 16) & 0xff) / (double) 0xff;
			        double green = (double)((pixel >> 8) & 0xff) / (double) 0xff;
			        double blue = (double)((pixel >> 0) & 0xff) / (double) 0xff;
			        
			        color = MathUtil.blendColors(new Vector4f(red, green, blue, alpha), color);
				}
			}
			
			return color;
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

		public String getId() {
			return id;
		}

		public String getBase64() {
			return base64;
		}

		public boolean isHalfTransparent() {
			return halfTransparent;
		}
		
		public Vector4f getColor(){
			return color;
		}
		
	}
	
}
