/**
 *    This code will be embedded into the HTML page as "normal"
 *    JavaScript code though a <script> tag. This allows one to
 *    interact with the page as any normal JavaScript code can.
 */
 
 // called once the page has fully loaded
 window.onload = function () {
     makeTheLink();
 }
 
 // make the connection with the sketch
 function makeTheLink() {
     
     // Get the instance. We just use the first one. Another way would be to use
     // the automaticaly generated ID "colorFinder", see <canvas> in index.html
     var mySketchInstance = Processing.getInstanceById( getProcessingSketchId() );
     
     if ( mySketchInstance == undefined ) { // means it has not started
         setTimeout( makeTheLink, 200 );    // try again later
         
     } else {
         mySketchInstance.setInterfaceLink(this); // make the connection
     }
 }
 
 // called from the sketch!
 function setColor ( rr, gg, bb ) {
     
     var colorString = 'rgb('+rr+','+gg+','+bb+')';
     
     document.body.style.color = colorString;
     
     var h1s = document.getElementsByTagName("h1");
     if ( h1s != undefined && h1s.length > 0 )
     {
         h1s[0].innerHTML = "Color is: " + colorString;
     }
     
     var links = document.links;
     if ( links.length > 0 )
     {
         for ( var l in links ) {
             links[l].style.color = colorString;
         }
     }
 }
