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
package de.bluecolored.bluemap.common.plugin.text;

public enum TextColor {

	BLACK ("black", '0'), 
	DARK_BLUE ("dark_blue", '1'), 
	DARK_GREEN ("dark_green", '2'), 
	DARK_AQUA ("dark_aqua", '3'), 
	DARK_RED ("dark_red", '4'),
	DARK_PURPLE ("dark_purple", '5'), 
	GOLD ("gold", '6'), 
	GRAY ("gray", '7'), 
	DARK_GRAY ("dark_gray", '8'), 
	BLUE ("blue", '9'), 
	GREEN ("green", 'a'),
	AQUA ("aqua", 'b'), 
	RED ("red", 'c'), 
	LIGHT_PURPLE ("light_purple", 'd'), 
	YELLOW ("yellow", 'e'), 
	WHITE ("white", 'f');

	private final String id;
	private final char formattingCode;
	
	private TextColor(String id, char formattingCode) {
		this.id = id;
		this.formattingCode = formattingCode;
	}
	
	public String getId() {
		return id;
	}
	
	public char getFormattingCode() {
		return formattingCode;
	}

}
