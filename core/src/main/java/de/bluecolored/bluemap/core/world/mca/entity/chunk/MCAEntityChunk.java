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
package de.bluecolored.bluemap.core.world.mca.entity.chunk;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.core.world.Entity;
import de.bluecolored.bluenbt.NBTName;
import lombok.Getter;

@Getter
public class MCAEntityChunk {

    private static final Entity[] EMPTY_ENTITIES = new Entity[0];

    public static final MCAEntityChunk EMPTY_CHUNK = new MCAEntityChunk();
    public static final MCAEntityChunk ERRORED_CHUNK = new MCAEntityChunk();

    @NBTName("Entities")
    public Entity[] entities = EMPTY_ENTITIES;

    @NBTName("DataVersion")
    public int dataVersion = -1;

    @NBTName("Position")
    public Vector2i position = Vector2i.ZERO;

}
