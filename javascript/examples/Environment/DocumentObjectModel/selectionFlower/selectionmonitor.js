/**
 *    This code will be embedded into the HTML page as "normal"
 *    JavaScript code through a <script> tag. This allows one to
 *    interact with the page as any normal JavaScript code can.
 */

var mySketchInstance;

// called once the page has fully loaded
window.onload = function () {
    getSketchInstance();
}

// this is called (repeatedly) to find the sketch
function getSketchInstance() {
    var s = Processing.getInstanceById("selectionFlower");
    if ( s == undefined )
        return setTimeout(getSketchInstance, 200); // try again a bit later

    mySketchInstance = s;
    monitorSelection();
}

// this code gets called all the time (every 1/5 sec) to check
// if the selection changed
function monitorSelection ( ) {
    
    var txt = undefined;
    
    if (window.getSelection) {
        
        txt = window.getSelection().toString();
        
    } else if (document.getSelection) {
        
        txt = document.getSelection().toString();
        
    } else if (document.selection) {
        
        txt = document.selection.createRange().text;
    }
    
    if ( txt !== undefined && txt != "" )
    {
        mySketchInstance.setSelectionText(txt);  // set the text in the sketch
    }
    
    setTimeout(monitorSelection, 1000/5);
}
