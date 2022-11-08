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
package de.bluecolored.bluemap.core.world;

import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.util.Tristate;

@DebugDump
public class BlockProperties {

    public static final BlockProperties DEFAULT = new BlockProperties();

    private Tristate culling, occluding, alwaysWaterlogged, randomOffset, cullingIdentical;

    public BlockProperties() {
        this.culling = Tristate.UNDEFINED;
        this.occluding = Tristate.UNDEFINED;
        this.alwaysWaterlogged = Tristate.UNDEFINED;
        this.randomOffset = Tristate.UNDEFINED;
        this.cullingIdentical = Tristate.UNDEFINED;
    }

    public BlockProperties(
            Tristate culling,
            Tristate occluding,
            Tristate alwaysWaterlogged,
            Tristate randomOffset,
            Tristate cullingIdentical
    ) {
        this.culling = culling;
        this.occluding = occluding;
        this.alwaysWaterlogged = alwaysWaterlogged;
        this.randomOffset = randomOffset;
        this.cullingIdentical = cullingIdentical;
    }

    public boolean isCulling() {
        return culling.getOr(true);
    }

    public boolean isOccluding() {
        return occluding.getOr(true);
    }

    public boolean isAlwaysWaterlogged() {
        return alwaysWaterlogged.getOr(false);
    }

    public boolean isRandomOffset() {
        return randomOffset.getOr(false);
    }

    public boolean getCullingIdentical() {
        return cullingIdentical.getOr(false);
    }

    public Builder toBuilder() {
        return new BlockProperties(
                culling,
                occluding,
                alwaysWaterlogged,
                randomOffset,
                cullingIdentical
        ).new Builder();
    }

    public static Builder builder() {
        return new BlockProperties().new Builder();
    }

    public class Builder {

        public Builder culling(boolean culling) {
            BlockProperties.this.culling = culling ? Tristate.TRUE : Tristate.FALSE;
            return this;
        }

        public Builder occluding(boolean occluding) {
            BlockProperties.this.occluding = occluding ? Tristate.TRUE : Tristate.FALSE;
            return this;
        }

        public Builder alwaysWaterlogged(boolean alwaysWaterlogged) {
            BlockProperties.this.alwaysWaterlogged = alwaysWaterlogged ? Tristate.TRUE : Tristate.FALSE;
            return this;
        }

        public Builder randomOffset(boolean randomOffset) {
            BlockProperties.this.randomOffset = randomOffset ? Tristate.TRUE : Tristate.FALSE;
            return this;
        }

        public Builder cullingIdentical(boolean cullingIdentical) {
            BlockProperties.this.cullingIdentical = cullingIdentical ? Tristate.TRUE : Tristate.FALSE;
            return this;
        }

        public Builder from(BlockProperties other) {
            culling = other.culling.getOr(culling);
            occluding = other.occluding.getOr(occluding);
            alwaysWaterlogged = other.alwaysWaterlogged.getOr(alwaysWaterlogged);
            randomOffset = other.randomOffset.getOr(randomOffset);
            cullingIdentical = other.cullingIdentical.getOr(cullingIdentical);
            return this;
        }

        public BlockProperties build() {
            return BlockProperties.this;
        }

        public Tristate isCulling() {
            return culling;
        }

        public Tristate isOccluding() {
            return occluding;
        }

        public Tristate isAlwaysWaterlogged() {
            return alwaysWaterlogged;
        }

        public Tristate isRandomOffset() {
            return randomOffset;
        }

        public Tristate isCullingIdentical() {
            return cullingIdentical;
        }

    }

    @Override
    public String toString() {
        return "BlockProperties{" +
               "culling=" + culling +
               ", occluding=" + occluding +
               ", alwaysWaterlogged=" + alwaysWaterlogged +
               ", randomOffset=" + randomOffset +
               ", cullingIdentical=" + cullingIdentical +
               '}';
    }

}
