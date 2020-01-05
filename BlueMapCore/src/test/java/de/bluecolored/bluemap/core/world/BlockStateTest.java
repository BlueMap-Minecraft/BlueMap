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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BlockStateTest {

	@Test
	public void testIdNamespace() {
		BlockState blockState = new BlockState("someblock");
		assertEquals("minecraft:someblock", blockState.getFullId());
		assertEquals("minecraft", blockState.getNamespace());
		assertEquals("someblock", blockState.getId());

		blockState = new BlockState("somemod:someblock");
		assertEquals("somemod:someblock", blockState.getFullId());
		assertEquals("somemod", blockState.getNamespace());
		assertEquals("someblock", blockState.getId());
	}

	@Test
	public void testToString() {
		BlockState blockState = new BlockState("someblock");
		assertEquals("minecraft:someblock[]", blockState.toString());
		
		blockState = blockState.with("testProp", "testVal");
		assertEquals("minecraft:someblock[testProp=testVal]", blockState.toString());

		blockState = blockState.with("testProp2", "testVal2");
		String toString = blockState.toString();
		assertTrue(
				toString.equals("minecraft:someblock[testProp=testVal,testProp2=testVal2]") ||
				toString.equals("minecraft:someblock[testProp2=testVal2,testProp=testVal]")
			);
	}
	

	@Test
	public void testFromString() {
		BlockState blockState = BlockState.fromString("somemod:someblock");
		assertEquals("somemod:someblock", blockState.getFullId());
		assertEquals("somemod", blockState.getNamespace());
		assertEquals("someblock", blockState.getId());
		assertTrue(blockState.getProperties().isEmpty());
		
		blockState = BlockState.fromString("somemod:someblock[]");
		assertEquals("somemod:someblock", blockState.getFullId());
		assertEquals("somemod", blockState.getNamespace());
		assertEquals("someblock", blockState.getId());
		assertTrue(blockState.getProperties().isEmpty());
		
		blockState = BlockState.fromString("somemod:someblock[testProp=testVal,testProp2=testVal2]");
		assertEquals("somemod:someblock", blockState.getFullId());
		assertEquals("somemod", blockState.getNamespace());
		assertEquals("someblock", blockState.getId());
		assertEquals("testVal", blockState.getProperties().get("testProp"));
		assertEquals("testVal2", blockState.getProperties().get("testProp2"));
	}
	
}
