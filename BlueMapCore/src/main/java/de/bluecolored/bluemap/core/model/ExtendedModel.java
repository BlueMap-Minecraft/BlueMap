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
package de.bluecolored.bluemap.core.model;

import de.bluecolored.bluemap.core.threejs.BufferAttribute;
import de.bluecolored.bluemap.core.threejs.BufferGeometry;

@Deprecated
public class ExtendedModel extends Model<ExtendedFace> {

    @Override
    public BufferGeometry toBufferGeometry() {
        BufferGeometry geo = super.toBufferGeometry();

        int count = getFaces().size();
        float[] ao = new float[count * 3];
        float[] blockLight = new float[count * 3];
        float[] sunLight = new float[count * 3];

        for (int itemIndex = 0; itemIndex < count; itemIndex++){
            ExtendedFace f = getFaces().get(itemIndex);
            ao[itemIndex * 3 + 0] = f.getAo1();
            ao[itemIndex * 3 + 1] = f.getAo2();
            ao[itemIndex * 3 + 2] = f.getAo3();

            blockLight[itemIndex * 3 + 0] = f.getBl1();
            blockLight[itemIndex * 3 + 1] = f.getBl2();
            blockLight[itemIndex * 3 + 2] = f.getBl3();

            sunLight[itemIndex * 3 + 0] = f.getSl1();
            sunLight[itemIndex * 3 + 1] = f.getSl2();
            sunLight[itemIndex * 3 + 2] = f.getSl3();
        }

        geo.addAttribute("ao", new BufferAttribute(ao, 1));
        geo.addAttribute("blocklight", new BufferAttribute(blockLight, 1));
        geo.addAttribute("sunlight", new BufferAttribute(sunLight, 1));

        return geo;
    }

}
