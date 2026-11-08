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
import {BufferAttribute, BufferGeometry, Matrix4, Mesh, Vector3} from "three";
import {Line2} from "three/examples/jsm/lines/Line2";
import {LineSegmentsGeometry} from "three/examples/jsm/lines/LineSegmentsGeometry";

const MIN_SHAPE_MARKERS = 64;
const HIDDEN_LAYER = 30;

export class ShapeMarkerBatch {

    /**
     * @param markerSet {MarkerSet}
     */
    constructor(markerSet) {
        this.markerSet = markerSet;
        this.objects = [];
        this.parts = [];
    }

    update() {
        this.clear();

        let markers = [...this.markerSet.markers.values()].filter(marker => marker.isShapeMarker && marker.visible);
        if (markers.length < MIN_SHAPE_MARKERS) return;

        this.markerSet.updateWorldMatrix(true, true);
        let toSet = new Matrix4().copy(this.markerSet.matrixWorld).invert();

        let groups = new Map();
        markers.forEach(marker => {
            [marker.fill, marker.border].forEach(part => {
                if (!part.visible) return;
                let key = batchKey(part);
                let group = groups.get(key);
                if (group) group.push(part);
                else groups.set(key, [part]);
            });
        });

        groups.forEach(parts => {
            let object = parts[0].isLine2 ? createBorderBatch(parts, toSet) : createFillBatch(parts, toSet);
            this.objects.push(object);
            this.markerSet.add(object);
            parts.forEach(part => {
                part.layers.set(HIDDEN_LAYER);
                this.parts.push(part);
            });
        });
    }

    clear() {
        this.objects.forEach(object => {
            this.markerSet.remove(object);
            object.dispose();
        });
        this.parts.forEach(part => part.layers.set(0));
        this.objects = [];
        this.parts = [];
    }

}

function batchKey(part) {
    let color = part.color;
    return [
        part.isLine2 ? "border" : "fill",
        part.renderOrder,
        part.depthTest,
        color.r, color.g, color.b,
        part.opacity,
        part.isLine2 ? part.linewidth : 0,
        part.fadeDistanceMin,
        part.fadeDistanceMax
    ].join("|");
}

function createFillBatch(parts, toSet) {
    let vertexCount = 0;
    let indexCount = 0;
    parts.forEach(part => {
        let position = part.geometry.attributes.position;
        vertexCount += position.count;
        indexCount += part.geometry.index ? part.geometry.index.count : position.count;
    });

    let positions = new Float32Array(vertexCount * 3);
    let indices = new Uint32Array(indexCount);
    let matrix = new Matrix4();
    let point = new Vector3();
    let v = 0;
    let i = 0;
    parts.forEach(part => {
        matrix.multiplyMatrices(toSet, part.matrixWorld);
        let position = part.geometry.attributes.position;
        for (let k = 0; k < position.count; k++) {
            point.fromBufferAttribute(position, k).applyMatrix4(matrix).toArray(positions, (v + k) * 3);
        }

        let index = part.geometry.index;
        if (index) {
            for (let k = 0; k < index.count; k++) indices[i++] = v + index.getX(k);
        } else {
            for (let k = 0; k < position.count; k++) indices[i++] = v + k;
        }

        v += position.count;
    });

    let geometry = new BufferGeometry();
    geometry.setAttribute("position", new BufferAttribute(positions, 3));
    geometry.setIndex(new BufferAttribute(indices, 1));

    return createBatchObject(new Mesh(geometry, parts[0].material.clone()), parts[0]);
}

function createBorderBatch(parts, toSet) {
    let segmentCount = parts.reduce((sum, part) => sum + part.geometry.attributes.instanceStart.count, 0);

    let positions = new Float32Array(segmentCount * 6);
    let matrix = new Matrix4();
    let point = new Vector3();
    let s = 0;
    parts.forEach(part => {
        matrix.multiplyMatrices(toSet, part.matrixWorld);
        let start = part.geometry.attributes.instanceStart;
        let end = part.geometry.attributes.instanceEnd;
        for (let k = 0; k < start.count; k++, s++) {
            point.fromBufferAttribute(start, k).applyMatrix4(matrix).toArray(positions, s * 6);
            point.fromBufferAttribute(end, k).applyMatrix4(matrix).toArray(positions, s * 6 + 3);
        }
    });

    let geometry = new LineSegmentsGeometry();
    geometry.setPositions(positions);

    let line = createBatchObject(new Line2(geometry, parts[0].material.clone()), parts[0]);
    line.onBeforeRender = renderer => {
        let resolution = line.material.resolution;
        renderer.getSize(resolution);
        if (parts[0].material.resolution.equals(resolution)) return;
        parts.forEach(part => part.material.resolution.copy(resolution));
    };

    return line;
}

function createBatchObject(object, part) {
    object.renderOrder = part.renderOrder;
    object.raycast = () => {};
    object.dispose = () => {
        object.geometry.dispose();
        object.material.dispose();
    };
    return object;
}
