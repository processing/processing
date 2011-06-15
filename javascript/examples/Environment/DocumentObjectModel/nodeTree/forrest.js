
// wait for document to load
window.onload = function () {
    tryFindSketch();
}

function tryFindSketch () {
    var sketch = Processing.instances[0];
    if ( sketch == undefined )
        return setTimeout(tryFindSketch, 200);
    
    sketch.setTree( document.body.parentNode );
}
