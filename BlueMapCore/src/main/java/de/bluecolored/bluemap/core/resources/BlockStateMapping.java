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
package de.bluecolored.bluemap.core.resources;

import de.bluecolored.bluemap.core.debug.DebugDump;
import de.bluecolored.bluemap.core.world.BlockState;

import java.util.Map.Entry;

@DebugDump
class BlockStateMapping<T> {
    private final BlockState blockState;
    private final T mapping;

    public BlockStateMapping(BlockState blockState, T mapping) {
        this.blockState = blockState;
        this.mapping = mapping;
    }

    /**
     * Returns true if the all the properties on this BlockMapping-key are the same in the provided BlockState.<br>
     * Properties that are not defined in this Mapping are ignored on the provided BlockState.<br>
     */
    public boolean fitsTo(BlockState blockState){
        if (!this.blockState.getValue().equals(blockState.getValue())) return false;
        for (Entry<String, String> e : this.blockState.getProperties().entrySet()){
            if (!e.getValue().equals(blockState.getProperties().get(e.getKey()))){
                return false;
            }
        }

        return true;
    }

    public BlockState getBlockState(){
        return blockState;
    }

    public T getMapping(){
        return mapping;
    }

}