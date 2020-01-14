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
import $ from 'jquery';

import { getTopRightElement } from './Module.js';

export default class Info {
  constructor(blueMap) {
    this.blueMap = blueMap;
    const parent = getTopRightElement(blueMap);
    $('#bluemap-info').remove();
    this.elementInfo = $('<div id="bluemap-info" class="button"></div>').appendTo(parent);
    this.elementInfo.click(this.onClick);
  }

  onClick = () => {
    this.blueMap.alert(
      '<h1>Info</h1>' +
      'Visit BlueMap on <a href="https://github.com/BlueMap-Minecraft">GitHub</a>!<br>' +
      'BlueMap works best with <a href="https://www.google.com/chrome/">Chrome</a>.<br>' +
      '<h2>Controls</h2>' +
      'Leftclick-drag with your mouse or use the arrow-keys to navigate.<br>' +
      'Rightclick-drag with your mouse to rotate your view.<br>' +
      'Scroll to zoom.<br>'
    );
  }
}
