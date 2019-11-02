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
package de.bluecolored.bluemap.core.util;

import java.util.ArrayList;
import java.util.List;

public class WeighedArrayList<E> extends ArrayList<E> implements List<E> {
	private static final long serialVersionUID = 1L;
	
	public WeighedArrayList() {}
	
	public WeighedArrayList(int capacity) {
		super(capacity);
	}
	
	/**
	 * Adds the element weight times to this list.
	 * @return Always true
	 */
	public void add(E e, int weight) {
		for (int i = 0; i < weight; i++){
			add(e);
		}
	}
	
	/**
	 * Removes the first weight number of items that equal o from this list.<br>
	 * @return The number of elements removed.
	 */
	public int remove(Object o, int weight) {
		int removed = 0;
		if (o == null){
			for (int i = 0; i < size(); i++){
				if (get(i) == null){
					remove(i);
					removed++;
					if (removed >= weight) break;
				}
			}
		} else {
			for (int i = 0; i < size(); i++){
				if (o.equals(get(i))){
					remove(i);
					removed++;
					if (removed >= weight) break;
				}
			}
		}
		
		return removed;
	}
	
}
