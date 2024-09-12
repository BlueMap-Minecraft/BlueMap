/**
 * Taken from https://github.com/mrdoob/three.js/blob/master/examples/jsm/libs/stats.module.js
 */
let Stats = function () {

    let mode = 0;

    let container = document.createElement( 'div' );
    container.style.cssText = 'position:absolute;bottom:5px;right:5px;cursor:pointer;opacity:0.9;z-index:10000';
    container.addEventListener( 'click', function ( event ) {

        event.preventDefault();
        showPanel( ++ mode % container.children.length );

    }, false );

    //

    function addPanel( panel ) {

        container.appendChild( panel.dom );
        return panel;

    }

    function showPanel( id ) {

        for ( let i = 0; i < container.children.length; i ++ ) {

            container.children[ i ].style.display = i === id ? 'block' : 'none';

        }

        mode = id;

    }

    function hide() {
        showPanel(-1);
    }

    //

    let beginTime = ( performance || Date ).now(), prevTime = beginTime, frames = 0;
    let prevFrameTime = beginTime;

    let fpsPanel = addPanel( new Stats.Panel( 'FPS', '#0ff', '#002' ) );
    let msPanel = addPanel( new Stats.Panel( 'MS (render)', '#0f0', '#020' ) );
    let lastFrameMsPanel = addPanel( new Stats.Panel( 'MS (all)', '#f80', '#210' ) );

    let memPanel = null;
    if ( self.performance && self.performance.memory ) {

        memPanel = addPanel( new Stats.Panel( 'MB', '#f08', '#201' ) );

    }

    showPanel( 0 );

    return {

        REVISION: 16,

        dom: container,

        addPanel: addPanel,
        showPanel: showPanel,
        hide: hide,

        begin: function () {

            beginTime = ( performance || Date ).now();

        },

        end: function () {

            frames ++;

            let time = ( performance || Date ).now();

            msPanel.update( time - beginTime, 200 );
            lastFrameMsPanel.update( time - prevFrameTime, 200 )

            if ( time >= prevTime + 1000 ) {

                fpsPanel.update( ( frames * 1000 ) / ( time - prevTime ), 100 );

                prevTime = time;
                frames = 0;

                if ( memPanel ) {

                    let memory = performance.memory;
                    memPanel.update( memory.usedJSHeapSize / 1048576, memory.jsHeapSizeLimit / 1048576 );

                }

            }

            return time;

        },

        update: function () {

            beginTime = this.end();
            prevFrameTime = beginTime;

        },

        // Backwards Compatibility

        domElement: container,
        setMode: showPanel

    };

};

Stats.Panel = function ( name, fg, bg ) {

    let min = Infinity, max = 0, round = Math.round;
    let PR = round( window.devicePixelRatio || 1 );

    let WIDTH = 160 * PR, HEIGHT = 96 * PR,
        TEXT_X = 3 * PR, TEXT_Y = 3 * PR,
        GRAPH_X = 3 * PR, GRAPH_Y = 15 * PR,
        GRAPH_WIDTH = 154 * PR, GRAPH_HEIGHT = 77 * PR;

    let canvas = document.createElement( 'canvas' );
    canvas.width = WIDTH;
    canvas.height = HEIGHT;
    canvas.style.cssText = 'width:160px;height:96px';

    let context = canvas.getContext( '2d' );
    context.font = 'bold ' + ( 9 * PR ) + 'px Helvetica,Arial,sans-serif';
    context.textBaseline = 'top';

    context.fillStyle = bg;
    context.fillRect( 0, 0, WIDTH, HEIGHT );

    context.fillStyle = fg;
    context.fillText( name, TEXT_X, TEXT_Y );
    context.fillRect( GRAPH_X, GRAPH_Y, GRAPH_WIDTH, GRAPH_HEIGHT );

    context.fillStyle = bg;
    context.globalAlpha = 0.9;
    context.fillRect( GRAPH_X, GRAPH_Y, GRAPH_WIDTH, GRAPH_HEIGHT );

    return {

        dom: canvas,

        update: function ( value, maxValue ) {

            min = Math.min( min, value );
            max = Math.max( max, value );

            context.fillStyle = bg;
            context.globalAlpha = 1;
            context.fillRect( 0, 0, WIDTH, GRAPH_Y );
            context.fillStyle = fg;
            context.fillText( round( value ) + ' ' + name + ' (' + round( min ) + '-' + round( max ) + ')', TEXT_X, TEXT_Y );

            context.drawImage( canvas, GRAPH_X + PR, GRAPH_Y, GRAPH_WIDTH - PR, GRAPH_HEIGHT, GRAPH_X, GRAPH_Y, GRAPH_WIDTH - PR, GRAPH_HEIGHT );

            context.fillRect( GRAPH_X + GRAPH_WIDTH - PR, GRAPH_Y, PR, GRAPH_HEIGHT );

            context.fillStyle = bg;
            context.globalAlpha = 0.9;
            context.fillRect( GRAPH_X + GRAPH_WIDTH - PR, GRAPH_Y, PR, round( ( 1 - ( value / maxValue ) ) * GRAPH_HEIGHT ) );

        }

    };

};

export default Stats;