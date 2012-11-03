
// wait for page contents to load: 
// add page load listener
window.onload = function () {
    tryFindSketch();
}

function tryFindSketch () {
    var sketch = Processing.getInstanceById(getProcessingSketchId());
    if ( sketch == undefined ) 
        return setTimeout(tryFindSketch, 200);  // try again ..
    
    // get slider from DOM
    var range = document.getElementById("form-range");
    // add listener
    range.onchange = function () {
        sketch.newRangeValue( range.value );
    }
}
