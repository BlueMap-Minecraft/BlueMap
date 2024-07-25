/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) Kevin Chapelier <https://github.com/kchapelier>
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
 *
 * Adapted version of PRWM by Kevin Chapelier
 * See https://github.com/kchapelier/PRWM for more informations about this file format
 */

import {
    DefaultLoadingManager,
    BufferGeometry,
    BufferAttribute,
    FloatType
} from "three"

"use strict";

let bigEndianPlatform = null;

/**
 * Check if the endianness of the platform is big-endian (most significant bit first)
 * @returns {boolean} True if big-endian, false if little-endian
 */
function isBigEndianPlatform() {
    if ( bigEndianPlatform === null ) {
        let buffer = new ArrayBuffer( 2 ),
            uint8Array = new Uint8Array( buffer ),
            uint16Array = new Uint16Array( buffer );

        uint8Array[ 0 ] = 0xAA; // set first byte
        uint8Array[ 1 ] = 0xBB; // set second byte
        bigEndianPlatform = ( uint16Array[ 0 ] === 0xAABB );
    }

    return bigEndianPlatform;
}

// match the values defined in the spec to the TypedArray types
let InvertedEncodingTypes = [
    null,
    Float32Array,
    null,
    Int8Array,
    Int16Array,
    null,
    Int32Array,
    Uint8Array,
    Uint16Array,
    null,
    Uint32Array
];

// define the method to use on a DataView, corresponding the TypedArray type
let getMethods = {
    Uint16Array: 'getUint16',
    Uint32Array: 'getUint32',
    Int16Array: 'getInt16',
    Int32Array: 'getInt32',
    Float32Array: 'getFloat32',
    Float64Array: 'getFloat64'
};

function copyFromBuffer( sourceArrayBuffer, viewType, position, length, fromBigEndian ) {
    let bytesPerElement = viewType.BYTES_PER_ELEMENT,
        result;

    if ( fromBigEndian === isBigEndianPlatform() || bytesPerElement === 1 ) {
        result = new viewType( sourceArrayBuffer, position, length );
    } else {
        console.debug("PRWM file has opposite encoding, loading will be slow...");

        let readView = new DataView( sourceArrayBuffer, position, length * bytesPerElement ),
            getMethod = getMethods[ viewType.name ],
            littleEndian = ! fromBigEndian,
            i = 0;

        result = new viewType( length );

        for ( ; i < length; i ++ ) {
            result[ i ] = readView[ getMethod ]( i * bytesPerElement, littleEndian );
        }
    }

    return result;
}

/**
 * @param buffer {ArrayBuffer}
 * @param offset {number}
 */
function decodePrwm( buffer, offset ) {
    offset = offset || 0;

    let array = new Uint8Array( buffer, offset ),
        version = array[ 0 ],
        flags = array[ 1 ],
        indexedGeometry = !! ( flags >> 7 & 0x01 ),
        indicesType = flags >> 6 & 0x01,
        bigEndian = ( flags >> 5 & 0x01 ) === 1,
        attributesNumber = flags & 0x1F,
        valuesNumber = 0,
        indicesNumber = 0;

    if ( bigEndian ) {
        valuesNumber = ( array[ 2 ] << 16 ) + ( array[ 3 ] << 8 ) + array[ 4 ];
        indicesNumber = ( array[ 5 ] << 16 ) + ( array[ 6 ] << 8 ) + array[ 7 ];
    } else {
        valuesNumber = array[ 2 ] + ( array[ 3 ] << 8 ) + ( array[ 4 ] << 16 );
        indicesNumber = array[ 5 ] + ( array[ 6 ] << 8 ) + ( array[ 7 ] << 16 );
    }

    /** PRELIMINARY CHECKS **/

    if ( offset / 4 % 1 !== 0 ) {
        throw new Error( 'PRWM decoder: Offset should be a multiple of 4, received ' + offset );
    }

    if ( version === 0 ) {
        throw new Error( 'PRWM decoder: Invalid format version: 0' );
    } else if ( version !== 1 ) {
        throw new Error( 'PRWM decoder: Unsupported format version: ' + version );
    }

    if ( ! indexedGeometry ) {
        if ( indicesType !== 0 ) {
            throw new Error( 'PRWM decoder: Indices type must be set to 0 for non-indexed geometries' );
        } else if ( indicesNumber !== 0 ) {
            throw new Error( 'PRWM decoder: Number of indices must be set to 0 for non-indexed geometries' );
        }
    }

    /** PARSING **/

    let pos = 8;

    let attributes = {},
        attributeName,
        char,
        attributeType,
        cardinality,
        encodingType,
        normalized,
        arrayType,
        values,
        indices,
        groups,
        next,
        i;

    for ( i = 0; i < attributesNumber; i ++ ) {
        attributeName = '';

        while ( pos < array.length ) {
            char = array[ pos ];
            pos ++;

            if ( char === 0 ) {
                break;
            } else {
                attributeName += String.fromCharCode( char );
            }
        }

        flags = array[ pos ];

        attributeType = flags >> 7 & 0x01;
        normalized = flags >> 6 & 0x01;
        cardinality = ( flags >> 4 & 0x03 ) + 1;
        encodingType = flags & 0x0F;
        arrayType = InvertedEncodingTypes[ encodingType ];

        pos ++;

        // padding to next multiple of 4
        pos = Math.ceil( pos / 4 ) * 4;

        values = copyFromBuffer( buffer, arrayType, pos + offset, cardinality * valuesNumber, bigEndian );

        pos += arrayType.BYTES_PER_ELEMENT * cardinality * valuesNumber;

        attributes[ attributeName ] = {
            type: attributeType,
            cardinality: cardinality,
            values: values,
            normalized: normalized === 1
        };
    }

    indices = null;
    if ( indexedGeometry ) {
        pos = Math.ceil( pos / 4 ) * 4;
        indices = copyFromBuffer(
            buffer,
            indicesType === 1 ? Uint32Array : Uint16Array,
            pos + offset,
            indicesNumber,
            bigEndian
        );
    }

    // read groups
    groups = [];
    pos = Math.ceil( pos / 4 ) * 4;
    while ( pos < array.length ) {
        next = read4ByteInt(array, pos);
        if (next === -1) {
            pos += 4;
            break;
        }
        groups.push({
            materialIndex:  next,
            start:          read4ByteInt(array, pos + 4),
            count:          read4ByteInt(array, pos + 8)
        });
        pos += 12;
    }

    return {
        version: version,
        attributes: attributes,
        indices: indices,
        groups: groups
    };
}

function read4ByteInt(array, pos) {
    return array[pos] |
        array[pos + 1] << 8 |
        array[pos + 2] << 16 |
        array[pos + 3] << 24;
}

export class PRBMLoader {

    constructor ( manager ) {
        this.manager = ( manager !== undefined ) ? manager : DefaultLoadingManager;
    }

    load ( url, onLoad, onProgress, onError ) {
        let scope = this;

        url = url.replace( /\*/g, isBigEndianPlatform() ? 'be' : 'le' );

        let loader = new FileLoader( scope.manager );
        loader.setPath( scope.path );
        loader.setResponseType( 'arraybuffer' );

        loader.load( url, function ( arrayBuffer ) {
            onLoad( scope.parse( arrayBuffer ) );
        }, onProgress, onError );
    }

    setPath ( value ) {
        this.path = value;
        return this;
    }

    parse ( arrayBuffer, offset ) {
        let data = decodePrwm( arrayBuffer, offset ),
            attributesKey = Object.keys( data.attributes ),
            bufferGeometry = new BufferGeometry(),
            attribute,
            bufferAttribute,
            i;

        for ( i = 0; i < attributesKey.length; i ++ ) {
            attribute = data.attributes[ attributesKey[ i ] ];
            bufferAttribute = new BufferAttribute( attribute.values, attribute.cardinality, attribute.normalized );
            bufferAttribute.gpuType = FloatType;
            bufferGeometry.setAttribute( attributesKey[ i ], bufferAttribute );
        }

        if ( data.indices !== null ) {
            bufferGeometry.setIndex( new BufferAttribute( data.indices, 1 ) );
        }

        bufferGeometry.groups = data.groups;

        return bufferGeometry;
    }

    isBigEndianPlatform () {
        return isBigEndianPlatform();
    }

}