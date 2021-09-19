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
package de.bluecolored.bluemap.core.resourcepack.blockmodel;

import com.flowpowered.math.TrigMath;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;
import com.flowpowered.math.vector.Vector4f;
import de.bluecolored.bluemap.core.resourcepack.ParseResourceException;
import de.bluecolored.bluemap.core.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.resourcepack.blockmodel.BlockModelResource.Element.Face;
import de.bluecolored.bluemap.core.resourcepack.fileaccess.FileAccess;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.util.math.Axis;
import de.bluecolored.bluemap.core.util.math.MatrixM4f;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

public class BlockModelResource {
    private static final double FIT_TO_BLOCK_SCALE_MULTIPLIER = 2 - Math.sqrt(2);

    private ModelType modelType = ModelType.NORMAL;

    private boolean culling = false;
    private boolean occluding = false;

    private final boolean ambientOcclusion = true; //TODO: wat?
    private final Collection<Element> elements = new ArrayList<>();
    private final Map<String, Texture> textures = new HashMap<>();

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
        private MatrixM4f rotationMatrix;
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

        public MatrixM4f getRotationMatrix() {
            return rotationMatrix;
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

            InputStream fileIn = sourcesAccess.readFile(modelPath);
            ConfigurationNode config = GsonConfigurationLoader.builder()
                    .source(() -> new BufferedReader(new InputStreamReader(fileIn, StandardCharsets.UTF_8)))
                    .build()
                    .load();

            for (Entry<Object, ? extends ConfigurationNode> entry : config.node("textures").childrenMap().entrySet()) {
                if (entry.getKey().equals(JSON_COMMENT)) continue;

                String key = entry.getKey().toString();
                String value = entry.getValue().getString();

                if (("#" + key).equals(value)) continue; // skip direct loop

                textures.putIfAbsent(key, value);
            }

            String parentPath = config.node("parent").getString();
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
                        blockModel = this.buildNoReset(parentPath, renderElements && config.node("elements").virtual(), topModelPath);
                    } catch (IOException ex) {
                        throw new ParseResourceException("Failed to load parent model " + parentPath + " of model " + topModelPath, ex);
                    }
                }
            }

            if (renderElements) {
                for (ConfigurationNode elementNode : config.node("elements").childrenList()) {
                    blockModel.elements.add(buildElement(blockModel, elementNode, topModelPath));
                }
            }

            for (String key : textures.keySet()) {
                try {
                    blockModel.textures.put(key, getTexture("#" + key));
                } catch (NoSuchElementException | FileNotFoundException ignore) {
                    // ignore this so unused textures can remain unresolved. See issue #147

                    //throw new ParseResourceException("Failed to map Texture key '" + key + "' for model '" + topModelPath + "': " + ex);
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

                        if (texture.getColorStraight().a < 1) {
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

            element.from = readVector3f(node.node("from"));
            element.to = readVector3f(node.node("to"));

            element.shade = node.node("shade").getBoolean(true);

            boolean fullElement = element.from.equals(FULL_CUBE_FROM) && element.to.equals(FULL_CUBE_TO);

            if (!node.node("rotation").virtual()) {
                element.rotation.angle = node.node("rotation", "angle").getFloat(0);
                element.rotation.axis = Axis.fromString(node.node("rotation", "axis").getString("x"));
                if (!node.node("rotation", "origin").virtual()) element.rotation.origin = readVector3f(node.node("rotation", "origin"));
                element.rotation.rescale = node.node("rotation", "rescale").getBoolean(false);

                // rotation matrix
                float angle = element.rotation.angle;
                Vector3i axis = element.rotation.axis.toVector();
                Vector3f origin = element.rotation.origin;
                boolean rescale = element.rotation.rescale;

                MatrixM4f rot = new MatrixM4f();
                if (angle != 0f) {
                    rot.translate(-origin.getX(), -origin.getY(), -origin.getZ());
                    rot.rotate(
                            angle,
                            axis.getX(),
                            axis.getY(),
                            axis.getZ()
                    );

                    if (rescale) {
                        float scale = (float) (Math.abs(TrigMath.sin(angle * TrigMath.DEG_TO_RAD)) * FIT_TO_BLOCK_SCALE_MULTIPLIER);
                        rot.scale(
                                (1 - axis.getX()) * scale + 1,
                                (1 - axis.getY()) * scale + 1,
                                (1 - axis.getZ()) * scale + 1
                        );
                    }

                    rot.translate(origin.getX(), origin.getY(), origin.getZ());
                }
                element.rotationMatrix = rot;
            } else {
                element.rotationMatrix = new MatrixM4f();
            }

            boolean allDirs = true;
            for (Direction direction : Direction.values()) {
                ConfigurationNode faceNode = node.node("faces", direction.name().toLowerCase());
                if (!faceNode.virtual()) {
                    try {
                        Face face = buildFace(element, direction, faceNode);
                        element.faces.put(direction, face);
                    } catch (ParseResourceException | IOException ex) {
                        throw new ParseResourceException("Failed to parse an " + direction + " face for the model " + topModelPath + "!", ex);
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

                if (!node.node("uv").virtual()) face.uv = readVector4f(node.node("uv"));
                face.texture = getTexture(node.node("texture").getString(""));
                face.tinted = node.node("tintindex").getInt(-1) >= 0;
                face.rotation = node.node("rotation").getInt(0);

                if (!node.node("cullface").virtual()) {
                    String dirString = node.node("cullface").getString("");
                    if (dirString.equals("bottom")) dirString = "down";
                    if (dirString.equals("top")) dirString = "up";

                    try {
                        face.cullface = Direction.fromString(dirString);
                    } catch (IllegalArgumentException ignore) {}
                }

                return face;
            } catch (FileNotFoundException ex) {
                throw new ParseResourceException("There is no texture with the path: " + node.node("texture").getString(), ex);
            } catch (NoSuchElementException ex) {
                throw new ParseResourceException("Texture key '" + node.node("texture").getString() + "' has no texture assigned!", ex);
            }
        }

        private Vector3f readVector3f(ConfigurationNode node) throws ParseResourceException {
            List<? extends ConfigurationNode> nodeList = node.childrenList();
            if (nodeList.size() < 3) throw new ParseResourceException("Failed to load Vector3: Not enough values in list-node!");

            return new Vector3f(
                    nodeList.get(0).getFloat(0),
                    nodeList.get(1).getFloat(0),
                    nodeList.get(2).getFloat(0)
                );
        }

        private Vector4f readVector4f(ConfigurationNode node) throws ParseResourceException {
            List<? extends ConfigurationNode> nodeList = node.childrenList();
            if (nodeList.size() < 4) throw new ParseResourceException("Failed to load Vector4: Not enough values in list-node!");

            return new Vector4f(
                    nodeList.get(0).getFloat(0),
                    nodeList.get(1).getFloat(0),
                    nodeList.get(2).getFloat(0),
                    nodeList.get(3).getFloat(0)
                );
        }

        private Texture getTexture(String key) throws NoSuchElementException, FileNotFoundException, IOException {
            return getTexture(key, 0);
        }

        private Texture getTexture(String key, int depth) throws NoSuchElementException, FileNotFoundException, IOException {
            if (key.isEmpty() || key.equals("#")) throw new NoSuchElementException("Empty texture key or name!");

            if (depth > 10) throw new NoSuchElementException("Recursive texture-variable!");

            if (key.charAt(0) == '#') {
                String value = textures.get(key.substring(1));
                if (value == null) throw new NoSuchElementException("There is no texture defined for the key " + key);
                return getTexture(value, depth + 1);
            }

            String path = ResourcePack.namespacedToAbsoluteResourcePath(key, "textures") + ".png";

            Texture texture;
            try {
                texture = resourcePack.getTextures().get(path);
            } catch (NoSuchElementException ex) {
                texture = resourcePack.getTextures().loadTexture(sourcesAccess, path);
            }

            return texture;
        }

    }

}
