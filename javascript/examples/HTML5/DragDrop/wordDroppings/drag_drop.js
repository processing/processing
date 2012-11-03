
// wait for document to load ...
window.onload = function () {
    tryLinkSketch();
}

// try and find the sketch
function tryLinkSketch() {
    var sketch = Processing.getInstanceById( getProcessingSketchId() );
    if ( sketch == undefined )
        setTimeout(tryLinkSketch, 200); /*try again later*/
    else {
        initDragDrop(sketch);
    }
}

// initialize DnD, add event listeners
function initDragDrop ( sketch ) {
  var target = sketch.externals.canvas;
  var targetPosition = getElementPosition(target);
  var messageType = 'text/plain';
    
  var links = document.querySelectorAll('.draggables > div'), el = null;
  for (var i = 0; i < links.length; i++) {
    el = links[i];
  
    el.setAttribute('draggable', 'true');
  
    addEvent(el, 'dragstart', function (e) {
      e.dataTransfer.effectAllowed = 'copy';
      var message = this.textContent || this.innerHTML;
      e.dataTransfer.setData( messageType, message );
      return false;
    });
  }
  
  addEvent( target, 'dragenter', function (e) {
      if (e.preventDefault) e.preventDefault();
      sketch.dragEnter();
      return false;
  });
  
  addEvent( target, 'dragover', function (e) {
      if (e.preventDefault) e.preventDefault();
      e.dataTransfer.dropEffect = 'copy';
      sketch.dragOver( e.pageX-targetPosition.x, 
                       e.pageY-targetPosition.y );
      return false;
  });

  addEvent( target, 'dragleave', function () {
    sketch.dragLeave();
  });

  addEvent( target, 'drop', function (e) {
      if (e.stopPropagation) e.stopPropagation();
      if (e.preventDefault) e.preventDefault();
      sketch.dragDrop( e.dataTransfer.getData(messageType), 
                       e.pageX-targetPosition.x, 
                       e.pageY-targetPosition.y );
      return false;
  });
}

// see: http://html5demos.com/drag
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

// see: http://www.quirksmode.org/js/findpos.html
function getElementPosition (obj) {
    var curleft = curtop = 0;
    if (obj.offsetParent) {
        do {
            curleft += obj.offsetLeft;
            curtop  += obj.offsetTop;
        } while (obj = obj.offsetParent);
        return {x:curleft,y:curtop};
    }
    return undefined;
}

