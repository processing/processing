
// wait for document to load
window.onload = function () {
    tryFindSketch();
}

// find sketch instance
function tryFindSketch () {
    var sketch = Processing.getInstanceById(getProcessingSketchId());
    if ( sketch == undefined )
        setTimeout( tryFindSketch, 200 ); // try again in 0.2 secs
    else
        sketch.setJS( this );
}

// called from inside the sketch
function promtForInput ( msg, def ) {
    return window.prompt( msg, def );
}
