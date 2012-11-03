
window.onload = function () {
    tryFindSketch();
}

function tryFindSketch() {
    var sketch = Processing.getInstanceById(getProcessingSketchId());
    if ( sketch == undefined )
        return setTimeout(tryFindSketch, 200); // retry soon
    
    sketch.console = console;
    initUploader(sketch);
}

function initUploader ( sketch ) {
    var uploadField = document.getElementById("file-input");
    
    uploadField.onchange = function (e) {
        e.preventDefault();
        
        var file = uploadField.files[0];
        var reader = new FileReader();
        
        reader.onload = function (event) {
            var img = new Image();
            img.onload = function (event2) {
                sketch.newImageAvailable(img);
            }
            img.src = event.target.result;
        };
        reader.readAsDataURL(file);
        
        return false;
    }
}
