
// wait for the DOM to become ready
window.onload = function () {
    
    // try to find the Processing sketch instance, or retry
    function tryFindSketch () {
        var sketch = Processing.getInstanceById(getProcessingSketchId());
        if ( sketch == undefined )
            setTimeout(tryFindSketch, 200); // retry in 0.2 secs
        else
            initVideos( sketch );
    }
    tryFindSketch();
}

// look for the video and send to sketch
function initVideos ( sketch ) {
    
    var video = document.getElementsByTagName("video")[0];

    video['loop'] = true; // as "loop" is not supported by many browsers
    
    var lastTime = -1;
    var lastFrame = null;
    
    // extending our HTMLVideoElement object to return a PImage
    video['getFrame'] = function () {
        if ( lastTime !== video.currentTime || !lastFrame )
        {
            lastFrame = new sketch.PImage;
            lastFrame.fromHTMLImageData(video);
            lastTime = video.currentTime;
            return lastFrame;
        } else {
            return lastFrame;
        }
    };
    
    // similar to tryFindSketch this creates a loop that
    // continues until the video becomes ready.
    (function( s, v ){
        var tryAddVideo = function () {
            if ( v.readyState > 0 ) {
                s.addVideo(v);
            } else {
                setTimeout(tryAddVideo, 200);
            }
        }
        tryAddVideo();
    })( sketch, video );
    
    // this is a temporary fix for Chrome browsers sometimes not looping videos ...
    if ( window.navigator.appVersion.toLowerCase().indexOf('chrome') >= 0 ) {
        addEvent(video,'timeupdate',function(){
            if ( video.currentTime == video.duration ) {
                video.src = video.currentSrc;
            }
        });
        addEvent(video,'canplay',function(){
            video.play();
        });
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
