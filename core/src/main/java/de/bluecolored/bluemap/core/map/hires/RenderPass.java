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
package de.bluecolored.bluemap.core.map.hires;

import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.map.TileMetaConsumer;
import de.bluecolored.bluemap.core.world.World;

public interface RenderPass {

    /**
     * Does a pass to render a specified of the world onto the given tileModel.
     * <p>
     *  <b>Implementation Note:</b><br>
     *  This method is guaranteed to be called only on <b>one thread per RenderPass instance</b>, so you can use this
     *  for optimizations.
     * </p>
     * @param world The world that should be rendered
     * @param modelMin The min-position of the world that should be included in the tileModel
     * @param modelMax The max-position of the world that should be included in the tileModel
     * @param modelAnchor The position in the world that should be at (0,0,0) in the tileModel
     * @param tileModel The model(-view) where the world should be rendered to.
     * @param tileMetaConsumer A consumer that the RenderPass can call to emit heightmap and light-data that is produced during rendering.
     *                         This data is then e.g. used to generate the lowres-tiles.
     */
    void render(World world, Vector3i modelMin, Vector3i modelMax, Vector3i modelAnchor, TileModelView tileModel, TileMetaConsumer tileMetaConsumer);

    /**
     * Does a pass to render a specified of the world onto the given tileModel.
     * @param world The world that should be rendered
     * @param modelMin The min-position of the world that should be included in the tileModel
     * @param modelMax The max-position of the world that should be included in the tileModel
     * @param modelAnchor The position in the world that should be at (0,0,0) in the tileModel
     * @param tileModel The model(-view) where the world should be rendered to.
     */
    default void render(World world, Vector3i modelMin, Vector3i modelMax, Vector3i modelAnchor, TileModelView tileModel) {
        render(world, modelMin, modelMax, modelAnchor, tileModel, (x, z, c, h, l) -> {});
    }

}
