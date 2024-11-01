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
package de.bluecolored.bluemap.core.map.hires.blockmodel;

import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;

public interface BlockRenderer {

    /**
     * Renders the given blocks (block-state-)variant into the given blockModel, and sets the given blockColor to the
     * color that represents the rendered block.
     * <p>
     *  <b>Implementation Note:</b><br>
     *  This method is guaranteed to be called only on <b>one thread per BlockRenderer instance</b>, so you can use this
     *  for optimizations.<br>
     *  Keep in mind this method will be called once for every block that is being rendered, so be very careful
     *  about performance and instance-creations.
     * </p>
     * @param block The block information that should be rendered.
     * @param variant The block-state variant that should be rendered.
     * @param blockModel The model(-view) where the block should be rendered to.
     * @param blockColor The color that should be set to the color that represents the rendered block.
     */
    void render(BlockNeighborhood<?> block, Variant variant, TileModelView blockModel, Color blockColor);

}
