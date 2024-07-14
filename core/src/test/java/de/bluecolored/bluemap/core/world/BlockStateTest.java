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

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BlockStateTest {

    @Test
    public void testIdNamespace() {
        BlockState blockState = new BlockState("someblock");
        assertEquals("minecraft:someblock", blockState.getFormatted());
        assertEquals("minecraft", blockState.getNamespace());
        assertEquals("someblock", blockState.getValue());

        blockState = new BlockState("somemod:someblock");
        assertEquals("somemod:someblock", blockState.getFormatted());
        assertEquals("somemod", blockState.getNamespace());
        assertEquals("someblock", blockState.getValue());
    }

    @Test
    public void testToString() {
        BlockState blockState = new BlockState("someblock");
        assertEquals("minecraft:someblock[]", blockState.toString());

        blockState = new BlockState("someblock", mapOf("testProp", "testVal"));
        assertEquals("minecraft:someblock[testProp=testVal]", blockState.toString());

        blockState = new BlockState("someblock", mapOf("testProp", "testVal", "testProp2", "testVal2"));
        String toString = blockState.toString();
        assertTrue(
                toString.equals("minecraft:someblock[testProp=testVal,testProp2=testVal2]") ||
                toString.equals("minecraft:someblock[testProp2=testVal2,testProp=testVal]")
            );
    }


    @Test
    public void testFromString() {
        BlockState blockState = BlockState.fromString("somemod:someblock");
        assertEquals("somemod:someblock", blockState.getFormatted());
        assertEquals("somemod", blockState.getNamespace());
        assertEquals("someblock", blockState.getValue());
        assertTrue(blockState.getProperties().isEmpty());

        blockState = BlockState.fromString("somemod:someblock[]");
        assertEquals("somemod:someblock", blockState.getFormatted());
        assertEquals("somemod", blockState.getNamespace());
        assertEquals("someblock", blockState.getValue());
        assertTrue(blockState.getProperties().isEmpty());

        blockState = BlockState.fromString("somemod:someblock[testProp=testVal,testProp2=testVal2]");
        assertEquals("somemod:someblock", blockState.getFormatted());
        assertEquals("somemod", blockState.getNamespace());
        assertEquals("someblock", blockState.getValue());
        assertEquals("testVal", blockState.getProperties().get("testProp"));
        assertEquals("testVal2", blockState.getProperties().get("testProp2"));
    }

    private <L, V> Map<L, V> mapOf(L key, V value) {
        Map<L, V> map = new HashMap<>();
        map.put(key, value);
        return Collections.unmodifiableMap(map);
    }

    private <L, V> Map<L, V> mapOf(L key, V value, L key2, V value2) {
        Map<L, V> map = new HashMap<>();
        map.put(key, value);
        map.put(key2, value2);
        return Collections.unmodifiableMap(map);
    }

}
