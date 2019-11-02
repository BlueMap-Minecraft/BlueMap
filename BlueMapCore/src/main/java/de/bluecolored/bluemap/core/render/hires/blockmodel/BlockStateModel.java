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
package de.bluecolored.bluemap.core.render.hires.blockmodel;

import com.flowpowered.math.vector.Vector4f;

import de.bluecolored.bluemap.core.model.Model;
import de.bluecolored.bluemap.core.util.MathUtil;

/**
 * A model with some extra information about the BlockState it represents
 */
public class BlockStateModel extends Model {

	private Vector4f mapColor;
	
	public BlockStateModel(){
		this(Vector4f.ZERO);
	}
	
	public BlockStateModel(Vector4f mapColor) {
		this.mapColor = mapColor;
	}

	@Override
	public void merge(Model model) {
		super.merge(model);
		
		if (model instanceof BlockStateModel){
			mergeMapColor(((BlockStateModel) model).getMapColor());
		}
	}
	
	public Vector4f getMapColor() {
		return mapColor;
	}

	public void setMapColor(Vector4f mapColor) {
		this.mapColor = mapColor;
	}
	
	public void mergeMapColor(Vector4f mapColor) {		
		this.mapColor = MathUtil.blendColors(this.mapColor, mapColor);
	}
	
}
