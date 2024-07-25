/**
 * @author mrdoob / http://mrdoob.com/
 *
 * adapted for bluemap's purposes
 */

import {
    Matrix4,
    Object3D, Vector2,
    Vector3
} from "three";
import {dispatchEvent} from "./Utils";

class CSS2DObject extends Object3D {

    constructor(element) {
        super();

        this.element = document.createElement("div");
        let parent = element.parentNode;
        parent.replaceChild(this.element, element);
        this.element.appendChild(element);

        this.element.style.position = 'absolute';

        this.anchor = new Vector2();

        this.events = null;

        this.addEventListener('removed', function () {

            this.traverse(function (object) {

                if (object.element instanceof Element && object.element.parentNode !== null) {

                    object.element.parentNode.removeChild(object.element);

                }

            });

        });

        let lastClick = -1;
        let handleClick = event => {
            let doubleTap = false;

            let now = Date.now();
            if (now - lastClick < 500) {
                doubleTap = true;
            }

            lastClick = now;

            let data = {doubleTap: doubleTap};

            if (this.onClick({event: event, data: data})) {
                event.preventDefault();
                event.stopPropagation();
            } else {
                // fire event
                dispatchEvent(this.events, "bluemapMapInteraction", {
                    data: data,
                    object: this,
                });
            }
        }

        this.element.addEventListener("click", handleClick);
        this.element.addEventListener("touch", handleClick);
    }

}

//

var CSS2DRenderer = function (events = null) {

    var _this = this;

    var _width, _height;
    var _widthHalf, _heightHalf;

    var vector = new Vector3();
    var viewMatrix = new Matrix4();
    var viewProjectionMatrix = new Matrix4();

    var cache = {
        objects: new WeakMap()
    };

    var domElement = document.createElement( 'div' );
    domElement.style.overflow = 'hidden';

    this.domElement = domElement;

    this.events = events;

    this.getSize = function () {

        return {
            width: _width,
            height: _height
        };

    };

    this.setSize = function ( width, height ) {

        _width = width;
        _height = height;

        _widthHalf = _width / 2;
        _heightHalf = _height / 2;

        domElement.style.width = width + 'px';
        domElement.style.height = height + 'px';

    };

    var renderObject = function ( object, scene, camera, parentVisible ) {

        if ( object instanceof CSS2DObject ) {

            object.events = _this.events;

            object.onBeforeRender( _this, scene, camera );

            vector.setFromMatrixPosition( object.matrixWorld );
            vector.applyMatrix4( viewProjectionMatrix );

            var element = object.element;
            var style = 'translate(' + ( vector.x * _widthHalf + _widthHalf - object.anchor.x) + 'px,' + ( - vector.y * _heightHalf + _heightHalf - object.anchor.y ) + 'px)';

            element.style.WebkitTransform = style;
            element.style.MozTransform = style;
            element.style.oTransform = style;
            element.style.transform = style;

            element.style.display = ( parentVisible && object.visible && vector.z >= - 1 && vector.z <= 1 && element.style.opacity !== "0" ) ? '' : 'none';

            var objectData = {
                distanceToCameraSquared: getDistanceToSquared( camera, object )
            };

            cache.objects.set( object, objectData );

            if ( element.parentNode !== domElement ) {

                domElement.appendChild( element );

            }

            object.onAfterRender( _this, scene, camera );

        }

        for ( var i = 0, l = object.children.length; i < l; i ++ ) {

            renderObject( object.children[ i ], scene, camera, parentVisible && object.visible );

        }

    };

    var getDistanceToSquared = function () {

        var a = new Vector3();
        var b = new Vector3();

        return function ( object1, object2 ) {

            a.setFromMatrixPosition( object1.matrixWorld );
            b.setFromMatrixPosition( object2.matrixWorld );

            return a.distanceToSquared( b );

        };

    }();

    var filterAndFlatten = function ( scene ) {

        var result = [];

        scene.traverse( function ( object ) {

            if ( object instanceof CSS2DObject ) result.push( object );

        } );

        return result;

    };

    var zOrder = function ( scene ) {

        var sorted = filterAndFlatten( scene ).sort( function ( a, b ) {

            var distanceA = cache.objects.get( a ).distanceToCameraSquared;
            var distanceB = cache.objects.get( b ).distanceToCameraSquared;

            return distanceA - distanceB;

        } );

        var zMax = sorted.length;

        for ( var i = 0, l = sorted.length; i < l; i ++ ) {

            let o = sorted[ i ];
            o.element.style.zIndex = o.disableDepthTest ? zMax + 1 : zMax - i;

        }

    };

    this.render = function ( scene, camera ) {

        if ( scene.matrixWorldAutoUpdate === true ) scene.updateMatrixWorld();
        if ( camera.parent === null ) camera.updateMatrixWorld();

        viewMatrix.copy( camera.matrixWorldInverse );
        viewProjectionMatrix.multiplyMatrices( camera.projectionMatrix, viewMatrix );

        renderObject( scene, scene, camera, true );
        zOrder( scene );

    };

};

export { CSS2DObject, CSS2DRenderer };