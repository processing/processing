
window.onload = function () {
    tryFindSketch();
}

function tryFindSketch () {
    var sketch = Processing.instances[0];
    if ( sketch == undefined )
        return setTimeout( tryFindSketch, 200 ); // try again in 0.2 secs
    
    sketch.setJS( this );
}

function promtForInput ( msg, def ) {
    return window.prompt( msg, def );
}
