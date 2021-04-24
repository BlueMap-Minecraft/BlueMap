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
import com.flowpowered.math.vector.Vector4f;
import de.bluecolored.bluemap.core.model.ExtendedModel;

import java.util.UUID;

/**
 * A model, containing additional information about the tile it represents
 */
public class HiresModel extends ExtendedModel {

	private UUID world;
	private Vector3i blockMin, blockMax, blockSize;

	private int[][] heights;
	private Vector4f[][] colors;
	
	public HiresModel(UUID world, Vector3i blockMin, Vector3i blockMax) {
		this.world = world;
		this.blockMin = blockMin;
		this.blockMax = blockMax;
		this.blockSize = blockMax.sub(blockMin).add(Vector3i.ONE);
		
		heights = new int[blockSize.getX()][blockSize.getZ()];
		colors = new Vector4f[blockSize.getX()][blockSize.getZ()];
	}
	
	public void setColor(int x, int z, Vector4f color){
		colors[x - blockMin.getX()][z - blockMin.getZ()] = color;
	}
	
	public Vector4f getColor(int x, int z){
		Vector4f color = colors[x - blockMin.getX()][z - blockMin.getZ()];
		if (color == null) return Vector4f.ZERO;
		return color;
	}
	
	public void setHeight(int x, int z, int height){
		heights[x - blockMin.getX()][z - blockMin.getZ()] = height;
	}
	
	public int getHeight(int x, int z){
		return heights[x - blockMin.getX()][z - blockMin.getZ()];
	}
	
	public UUID getWorld(){
		return world;
	}
	
	public Vector3i getBlockMin(){
		return blockMin;
	}
	
	public Vector3i getBlockMax(){
		return blockMax;
	}
	
	public Vector3i getBlockSize(){
		return blockSize;
	}
	
}
