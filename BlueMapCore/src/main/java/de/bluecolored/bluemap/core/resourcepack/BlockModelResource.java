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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector4f;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resourcepack.BlockModelResource.Element.Face;
import de.bluecolored.bluemap.core.resourcepack.fileaccess.FileAccess;
import de.bluecolored.bluemap.core.util.Axis;
import de.bluecolored.bluemap.core.util.Direction;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;

public class BlockModelResource {

	private ModelType modelType = ModelType.NORMAL;
	
	private boolean culling = false;
	private boolean occluding = false; 
	
	private boolean ambientOcclusion = true;
	private Collection<Element> elements = new ArrayList<>();
	private Map<String, Texture> textures = new HashMap<>();
	
	private BlockModelResource() {}
	
	public ModelType getType() {
		return modelType;
	}
	
	public boolean isAmbientOcclusion() {
		return ambientOcclusion;
	}
	
	public boolean isCulling() {
		return culling;
	}
	
	public boolean isOccluding() {
		return occluding;
	}

	public Collection<Element> getElements() {
		return elements;
	}
	
	public Texture getTexture(String key) {
		return textures.get(key);
	}

	public class Element {
		
		private Vector3f from = Vector3f.ZERO, to = new Vector3f(16f, 16f, 16f);
		private Rotation rotation = new Rotation();
		private boolean shade = true;
		private EnumMap<Direction, Face> faces = new EnumMap<>(Direction.class);
		private boolean fullCube = false;
		
		private Element() {}
		
		public Vector4f getDefaultUV(Direction face) {
			switch (face){
			
			case DOWN :
			case UP :
				return new Vector4f(
					from.getX(), from.getZ(),
					to.getX(),   to.getZ()
				);
				
			case NORTH :
			case SOUTH :
				return new Vector4f(
					from.getX(), from.getY(),
					to.getX(),   to.getY()
				);

			case WEST :
			case EAST :
				return new Vector4f(
					from.getZ(), from.getY(),
					to.getZ(),   to.getY()
				);
				
			default :
				return new Vector4f(
					0, 0, 
					16, 16
				);
			
			}
		}
		
		public BlockModelResource getModel() {
			return BlockModelResource.this;
		}
		
		public Vector3f getFrom() {
			return from;
		}

		public Vector3f getTo() {
			return to;
		}

		public Rotation getRotation() {
			return rotation;
		}

		public boolean isShade() {
			return shade;
		}
		
		public boolean isFullCube() {
			return fullCube;
		}

		public EnumMap<Direction, Face> getFaces() {
			return faces;
		}

		public class Face {
			
			private Vector4f uv;
			private Texture texture;
			private Direction cullface;
			private int rotation = 0;
			private boolean tinted = false;
			
			private Face(Direction dir) {
				uv = getDefaultUV(dir);
			}
			
			public Element getElement() {
				return Element.this;
			}

			public Vector4f getUv() {
				return uv;
			}

			public Texture getTexture() {
				return texture;
			}

			public Direction getCullface() {
				return cullface;
			}
			
			public int getRotation() {
				return rotation;
			}
			
			public boolean isTinted() {
				return tinted;
			}
			
		}
		
		public class Rotation {
			
			private Vector3f origin = new Vector3f(8, 8, 8);
			private Axis axis = Axis.Y;
			private float angle = 0;
			private boolean rescale = false;
			
			private Rotation() {}
			
			public Vector3f getOrigin() {
				return origin;
			}
			
			public Axis getAxis() {
				return axis;
			}
			
			public float getAngle() {
				return angle;
			}
			
			public boolean isRescale() {
				return rescale;
			}
			
		}

	}

	public static Builder builder(FileAccess sourcesAccess, ResourcePack resourcePack) {
		return new Builder(sourcesAccess, resourcePack);
	}
	
	public static class Builder {

		private static final String JSON_COMMENT = "__comment";
		private static final Vector3f FULL_CUBE_FROM = Vector3f.ZERO; 
		private static final Vector3f FULL_CUBE_TO = new Vector3f(16f, 16f, 16f);
		
		private FileAccess sourcesAccess;
		private ResourcePack resourcePack;
		
		private HashMap<String, String> textures;
		
		private Builder(FileAccess sourcesAccess, ResourcePack resourcePack) {
			this.sourcesAccess = sourcesAccess;
			this.resourcePack = resourcePack;
			
			this.textures = new HashMap<>();
		}

		public synchronized BlockModelResource build(String modelPath) throws IOException, ParseResourceException {
			return build(modelPath, Collections.emptyMap());
		}
		
		public synchronized BlockModelResource build(String modelPath, Map<String, String> textures) throws IOException, ParseResourceException {
			this.textures.clear();
			this.textures.putAll(textures);
			return buildNoReset(modelPath, true, modelPath);
		}
		
		private BlockModelResource buildNoReset(String modelPath, boolean renderElements, String topModelPath) throws IOException, ParseResourceException {
			BlockModelResource blockModel = new BlockModelResource();
			ConfigurationNode config = GsonConfigurationLoader.builder()
					.setSource(() -> new BufferedReader(new InputStreamReader(sourcesAccess.readFile(modelPath), StandardCharsets.UTF_8)))
					.build()
					.load();
			
			for (Entry<Object, ? extends ConfigurationNode> entry : config.getNode("textures").getChildrenMap().entrySet()) {
				if (entry.getKey().equals(JSON_COMMENT)) continue;
				
				textures.putIfAbsent(entry.getKey().toString(), entry.getValue().getString(null));
			}
			
			String parentPath = config.getNode("parent").getString();
			if (parentPath != null) {
				if (parentPath.startsWith("builtin")) {
					switch (parentPath) {
					case "builtin/liquid":
						blockModel.modelType = ModelType.LIQUID;
						break;
					}
				} else {
					try {
						parentPath = ResourcePack.namespacedToAbsoluteResourcePath(parentPath, "models") + ".json";
						blockModel = this.buildNoReset(parentPath, config.getNode("elements").isVirtual(), topModelPath);
					} catch (IOException ex) {
						Logger.global.logWarning("Failed to load parent model " + parentPath + " of model " + topModelPath + ": " + ex);
					}
				}
			}
			
			if (renderElements) {
				for (ConfigurationNode elementNode : config.getNode("elements").getChildrenList()) {
					try {
						blockModel.elements.add(buildElement(blockModel, elementNode, topModelPath));
					} catch (ParseResourceException ex) {
						Logger.global.logWarning("Failed to parse element of model " + modelPath + " (" + topModelPath + "): " + ex);
					}
				}
			}
			
			for (String key : textures.keySet()) {
				try {
					blockModel.textures.put(key, getTexture("#" + key));
				} catch (NoSuchElementException | FileNotFoundException ex) {
					Logger.global.logDebug("Failed to map Texture key '" + key + "': " + ex);
				}
			}
			
			//check block properties
			for (Element element : blockModel.elements) {
				if (element.isFullCube()) {
					blockModel.occluding = true;
					
					blockModel.culling = true;
					for (Direction dir : Direction.values()) {
						Face face = element.faces.get(dir);
						if (face == null) {
							blockModel.culling = false;
							break;
						}
						
						Texture texture = face.getTexture();
						if (texture == null) {
							blockModel.culling = false;
							break;
						}
						
						if (texture.getColor().getW() < 1) {
							blockModel.culling = false;
							break;
						}
					}
					
					break;
				}
			}
			
			return blockModel;
		}
		
		private Element buildElement(BlockModelResource model, ConfigurationNode node, String topModelPath) throws ParseResourceException {
			Element element = model.new Element();
			
			element.from = readVector3f(node.getNode("from"));
			element.to = readVector3f(node.getNode("to"));
			
			element.shade = node.getNode("shade").getBoolean(true);
			
			boolean fullElement = element.from.equals(FULL_CUBE_FROM) && element.to.equals(FULL_CUBE_TO);
			
			if (!node.getNode("rotation").isVirtual()) {
				element.rotation.angle = node.getNode("rotation", "angle").getFloat(0);
				element.rotation.axis = Axis.fromString(node.getNode("rotation", "axis").getString("x"));
				if (!node.getNode("rotation", "origin").isVirtual()) element.rotation.origin = readVector3f(node.getNode("rotation", "origin"));
				element.rotation.rescale = node.getNode("rotation", "rescale").getBoolean(false);
			}
			
			boolean allDirs = true;
			for (Direction direction : Direction.values()) {
				ConfigurationNode faceNode = node.getNode("faces", direction.name().toLowerCase());
				if (!faceNode.isVirtual()) {
					try {
						Face face = buildFace(element, direction, faceNode);
						element.faces.put(direction, face);
					} catch (ParseResourceException | IOException ex) {
						Logger.global.logDebug("Failed to parse an " + direction + " face for the model " + topModelPath + "! " + ex);
					}
				} else {
					allDirs = false;
				}
			}
			
			if (fullElement && allDirs) element.fullCube = true;
			
			return element;
		}
		
		private Face buildFace(Element element, Direction direction, ConfigurationNode node) throws ParseResourceException, IOException {
			try {
				Face face = element.new Face(direction);
				
				if (!node.getNode("uv").isVirtual()) face.uv = readVector4f(node.getNode("uv")); 
				face.texture = getTexture(node.getNode("texture").getString());
				face.tinted = node.getNode("tintindex").getInt(-1) >= 0;
				face.rotation = node.getNode("rotation").getInt(0);
				
				if (!node.getNode("cullface").isVirtual()) {
					String dirString = node.getNode("cullface").getString();
					if (dirString.equals("bottom")) dirString = "down";
					if (dirString.equals("top")) dirString = "up";
					try {
						face.cullface = Direction.fromString(dirString);
					} catch (IllegalArgumentException ignore) {}
				}
				
				return face;
			} catch (FileNotFoundException ex) {
				throw new ParseResourceException("There is no texture with the path: " + node.getNode("texture").getString(), ex);
			} catch (NoSuchElementException ex) {
				throw new ParseResourceException("Texture key '" + node.getNode("texture").getString() + "' has no texture assigned!", ex);
			}
		}
		
		private Vector3f readVector3f(ConfigurationNode node) throws ParseResourceException {
			List<? extends ConfigurationNode> nodeList = node.getChildrenList();
			if (nodeList.size() < 3) throw new ParseResourceException("Failed to load Vector3: Not enough values in list-node!");
			
			return new Vector3f(
					nodeList.get(0).getFloat(0),
					nodeList.get(1).getFloat(0),
					nodeList.get(2).getFloat(0)
				);
		}
		
		private Vector4f readVector4f(ConfigurationNode node) throws ParseResourceException {
			List<? extends ConfigurationNode> nodeList = node.getChildrenList();
			if (nodeList.size() < 4) throw new ParseResourceException("Failed to load Vector4: Not enough values in list-node!");
			
			return new Vector4f(
					nodeList.get(0).getFloat(0),
					nodeList.get(1).getFloat(0),
					nodeList.get(2).getFloat(0),
					nodeList.get(3).getFloat(0)
				);
		}
		
		private Texture getTexture(String key) throws NoSuchElementException, FileNotFoundException, IOException {
			if (key.charAt(0) == '#') {
				String value = textures.get(key.substring(1));
				if (value == null) throw new NoSuchElementException("There is no texture defined for the key " + key);
				return getTexture(value);
			}
			
			String path = ResourcePack.namespacedToAbsoluteResourcePath(key, "textures") + ".png";
			
			Texture texture;
			try {
				texture = resourcePack.textures.get(path);
			} catch (NoSuchElementException ex) {
				texture = resourcePack.textures.loadTexture(sourcesAccess, path);
			}
			
			return texture;
		}
		
	}
	
}
