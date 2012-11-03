
// wait for document to load
window.onload = function () {
    var audioNode = document.getElementsByTagName("audio")[0];
    
    function tryFindSketch () {
        var sketch = Processing.getInstanceById(getProcessingSketchId());
        if ( sketch == undefined )
            setTimeout(tryFindSketch,200); // retry
        else
            sketch.newAudio( audioNode );
    }
    
    addEvent(audioNode, 'canplaythrough', function () {
        tryFindSketch();
    });
}

// http://html5demos.com/
var addEvent = (function () {
  if (document.addEventListener) {
    return function (el, type, fn) {
      if (el && el.nodeName || el === window) {
        el.addEventListener(type, fn, false);
      } else if (el && el.length) {
        for (var i = 0; i < el.length; i++) {
          addEvent(el[i], type, fn);
        }
      }
    };
  } else {
    return function (el, type, fn) {
      if (el && el.nodeName || el === window) {
        el.attachEvent('on' + type, function () { return fn.call(el, window.event); });
      } else if (el && el.length) {
        for (var i = 0; i < el.length; i++) {
          addEvent(el[i], type, fn);
        }
      }
    };
  }
})();
