window.onload = function () {
    var video = document.getElementsByTagName("video")[0];
    video['loop'] = true;
    
    function tryFindSketch () {
        var sketch = Processing.getInstanceById(getProcessingSketchId());
        if ( sketch == undefined )
            return setTimeout(tryFindSketch, 200); // retry in 0.2 secs
        
        // extending our HTMLVideoElement object to return a PImage
        video['getFrame'] = function () {
            var img = new sketch.PImage;
            img.fromHTMLImageData(video);
            return img;
        };

        (function( s, v ){
            var tryAddVideo = function () {
                if ( v.readyState > 2 ) {
                    s.setVideo(v);
                } else {
                    setTimeout(tryAddVideo, 200);
                }
            }
            tryAddVideo();
        })( sketch, video );
    }
    
    // looping currently not working in chrome as our internal server does
    // not support http byte ranges
    addEvent(video,'ended',function(){
        if ( 'loop' in this && this.loop ) {
            this.currentTime = 0;
            this.play();
        }
    });
    
    tryFindSketch();
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
