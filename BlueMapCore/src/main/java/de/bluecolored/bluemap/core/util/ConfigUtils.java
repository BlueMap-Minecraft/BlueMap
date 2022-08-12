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

public class ConfigUtils {

    private ConfigUtils(){}

    /**
     * Returns a color-integer. The value can be an integer in String-Format or a string in hexadecimal format prefixed with # (css-style: e.g. #f16 becomes #ff1166).
     * @param val The String to parse
     * @return The parsed Integer
     * @throws NumberFormatException If the value is not formatted correctly or if there is no value present.
     */
    public static int parseColorFromString(String val) {
        if (val.charAt(0) == '#') {
            val = val.substring(1);
            if (val.length() == 3) val = val + "f";
            if (val.length() == 4) val = "" + val.charAt(0) + val.charAt(0) + val.charAt(1) + val.charAt(1) + val.charAt(2) + val.charAt(2) + val.charAt(3) + val.charAt(3);
            if (val.length() == 6) val = val + "ff";
            if (val.length() != 8) throw new NumberFormatException("Invalid color format!");
            val = val.substring(6, 8) + val.substring(0, 6); // move alpha to front
            return Integer.parseUnsignedInt(val, 16);
        }

        int color = Integer.parseInt(val);
        if ((color & 0xFF000000) == 0) color |= 0xFF000000; // assume full alpha if not present
        return color;
    }

}
