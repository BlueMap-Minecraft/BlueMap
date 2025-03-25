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
package de.bluecolored.bluemap.core.map.hires.entity;

import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector4f;
import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModel;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.entitystate.Part;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Element;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Face;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.util.math.MatrixM4f;
import de.bluecolored.bluemap.core.util.math.VectorM2f;
import de.bluecolored.bluemap.core.util.math.VectorM3f;
import de.bluecolored.bluemap.core.world.Entity;
import de.bluecolored.bluemap.core.world.LightData;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;

/**
 * This model builder creates a BlockStateModel using the information from parsed resource-pack json files.
 */
@SuppressWarnings("DuplicatedCode")
public class ResourceModelRenderer implements EntityRenderer {
    private static final float SCALE = 1f / 16f;

    final ResourcePack resourcePack;
    final TextureGallery textureGallery;
    final RenderSettings renderSettings;

    private final VectorM3f[] corners = new VectorM3f[8];
    private final VectorM2f[] rawUvs = new VectorM2f[4];
    private final VectorM2f[] uvs = new VectorM2f[4];
    private final Color tintColor = new Color();

    private Model modelResource;
    private TileModelView tileModel;
    private int sunLight, blockLight;
    private TintColorProvider tintProvider;

    @SuppressWarnings("unused")
    public ResourceModelRenderer(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings) {
        this.resourcePack = resourcePack;
        this.textureGallery = textureGallery;
        this.renderSettings = renderSettings;

        for (int i = 0; i < corners.length; i++) corners[i] = new VectorM3f(0, 0, 0);
        for (int i = 0; i < rawUvs.length; i++) rawUvs[i] = new VectorM2f(0, 0);
    }

    @Override
    public void render(Entity entity, BlockNeighborhood block, Part part, TileModelView tileModel) {
        render(
                entity,
                block,
                part.getModel().getResource(resourcePack::getModel),
                TintColorProvider.NO_TINT,
                tileModel
        );

        // apply transform
        if (part.isTransformed())
            tileModel.transform(part.getTransformMatrix());
    }

    void render(Entity entity, BlockNeighborhood block, Model model, TintColorProvider tintProvider, TileModelView tileModel) {
        this.modelResource = model;
        this.tileModel = tileModel;
        this.tintProvider = tintProvider;

        // light calculation
        LightData blockLightData = block.getLightData();
        this.sunLight = blockLightData.getSkyLight();
        this.blockLight = blockLightData.getBlockLight();

        // filter out entities that are in a "cave" that should not be rendered
        if (
                block.isRemoveIfCave() &&
                        (renderSettings.isCaveDetectionUsesBlockLight() ? Math.max(blockLight, sunLight) : sunLight) == 0
        ) return;

        // render model
        int modelStart = this.tileModel.getStart();

        Element[] elements = modelResource.getElements();
        if (elements != null) {
            for (Element element : elements) {
                buildModelElementResource(element, this.tileModel.initialize());
            }
        }

        this.tileModel.initialize(modelStart);

    }

    private final MatrixM4f modelElementTransform = new MatrixM4f();
    private void buildModelElementResource(Element element, TileModelView blockModel) {

        //create faces
        Vector3f from = element.getFrom();
        Vector3f to = element.getTo();

        float
                minX = Math.min(from.getX(), to.getX()),
                minY = Math.min(from.getY(), to.getY()),
                minZ = Math.min(from.getZ(), to.getZ()),
                maxX = Math.max(from.getX(), to.getX()),
                maxY = Math.max(from.getY(), to.getY()),
                maxZ = Math.max(from.getZ(), to.getZ());

        VectorM3f[] c = corners;
        c[0].x = minX; c[0].y = minY; c[0].z = minZ;
        c[1].x = minX; c[1].y = minY; c[1].z = maxZ;
        c[2].x = maxX; c[2].y = minY; c[2].z = minZ;
        c[3].x = maxX; c[3].y = minY; c[3].z = maxZ;
        c[4].x = minX; c[4].y = maxY; c[4].z = minZ;
        c[5].x = minX; c[5].y = maxY; c[5].z = maxZ;
        c[6].x = maxX; c[6].y = maxY; c[6].z = minZ;
        c[7].x = maxX; c[7].y = maxY; c[7].z = maxZ;

        int modelStart = blockModel.getStart();
        createElementFace(element, Direction.DOWN, c[0], c[2], c[3], c[1]);
        createElementFace(element, Direction.UP, c[5], c[7], c[6], c[4]);
        createElementFace(element, Direction.NORTH, c[2], c[0], c[4], c[6]);
        createElementFace(element, Direction.SOUTH, c[1], c[3], c[7], c[5]);
        createElementFace(element, Direction.WEST, c[0], c[1], c[5], c[4]);
        createElementFace(element, Direction.EAST, c[3], c[2], c[6], c[7]);
        blockModel.initialize(modelStart);

        //rotate and scale down
        blockModel.transform(modelElementTransform
                .copy(element.getRotation().getMatrix())
                .scale(SCALE, SCALE, SCALE)
        );
    }

    private void createElementFace(Element element, Direction faceDir, VectorM3f c0, VectorM3f c1, VectorM3f c2, VectorM3f c3) {
        Face face = element.getFaces().get(faceDir);
        if (face == null) return;

        // initialize the faces
        tileModel.initialize();
        tileModel.add(2);

        TileModel tileModel = this.tileModel.getTileModel();
        int face1 = this.tileModel.getStart();
        int face2 = face1 + 1;

        // ####### positions
        tileModel.setPositions(face1,
                c0.x, c0.y, c0.z,
                c1.x, c1.y, c1.z,
                c2.x, c2.y, c2.z
        );
        tileModel.setPositions(face2,
                c0.x, c0.y, c0.z,
                c2.x, c2.y, c2.z,
                c3.x, c3.y, c3.z
        );

        // ####### texture
        ResourcePath<Texture> texturePath = face.getTexture().getTexturePath(modelResource.getTextures()::get);
        int textureId = textureGallery.get(texturePath);
        tileModel.setMaterialIndex(face1, textureId);
        tileModel.setMaterialIndex(face2, textureId);

        // ####### UV
        Vector4f uvRaw = face.getUv();
        float
                uvx = uvRaw.getX() / 16f,
                uvy = uvRaw.getY() / 16f,
                uvz = uvRaw.getZ() / 16f,
                uvw = uvRaw.getW() / 16f;

        rawUvs[0].set(uvx, uvw);
        rawUvs[1].set(uvz, uvw);
        rawUvs[2].set(uvz, uvy);
        rawUvs[3].set(uvx, uvy);

        // face-rotation
        int rotationSteps = Math.floorDiv(face.getRotation(), 90) % 4;
        if (rotationSteps < 0) rotationSteps += 4;
        for (int i = 0; i < 4; i++)
            uvs[i] = rawUvs[(rotationSteps + i) % 4];

        tileModel.setUvs(face1,
                uvs[0].x, uvs[0].y,
                uvs[1].x, uvs[1].y,
                uvs[2].x, uvs[2].y
        );

        tileModel.setUvs(face2,
                uvs[0].x, uvs[0].y,
                uvs[2].x, uvs[2].y,
                uvs[3].x, uvs[3].y
        );

        // ####### face-tint
        if (face.getTintindex() >= 0) {
            tintProvider.setTintColor(face.getTintindex(), tintColor);
            tileModel.setColor(face1, tintColor.r, tintColor.g, tintColor.b);
            tileModel.setColor(face2, tintColor.r, tintColor.g, tintColor.b);
        } else {
            tileModel.setColor(face1, 1f, 1f, 1f);
            tileModel.setColor(face2, 1f, 1f, 1f);
        }

        // ####### blocklight
        int emissiveBlockLight = Math.max(blockLight, element.getLightEmission());
        tileModel.setBlocklight(face1, emissiveBlockLight);
        tileModel.setBlocklight(face2, emissiveBlockLight);

        // ####### sunlight
        tileModel.setSunlight(face1, sunLight);
        tileModel.setSunlight(face2, sunLight);

        // ######## AO
        tileModel.setAOs(face1, 1f, 1f, 1f);
        tileModel.setAOs(face2, 1f, 1f, 1f);

    }

    interface TintColorProvider {
        TintColorProvider NO_TINT = (index, color) -> color.set(1f, 1f, 1f, 1f, true);
        void setTintColor(int tintIndex, Color target);
    }

}
