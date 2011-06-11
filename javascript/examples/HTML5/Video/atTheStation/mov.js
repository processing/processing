window.onload = function () {
    var video = document.getElementsByTagName("video")[0];
    
    addEvent(video, 'canplaythrough', function(e) {
        tryFindSketch();
    }, false);
    
    function tryFindSketch () {
        var sketch = Processing.instances[0];
        if ( sketch == undefined )
            return setTimeout(tryFindSketch, 200); // retry in 0.2 secs

        sketch.setVideo( video );
    }
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
