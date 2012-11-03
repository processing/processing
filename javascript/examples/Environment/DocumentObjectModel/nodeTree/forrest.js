
// wait for document to load
window.onload = function () {
    tryFindSketch();
}

function tryFindSketch () {
    var sketch = Processing.getInstanceById( getProcessingSketchId() );
    if ( sketch == undefined )
        setTimeout(tryFindSketch, 200);  // retry after 0.2 secs
    else
        sketch.setTree( document.body.parentNode );
}
