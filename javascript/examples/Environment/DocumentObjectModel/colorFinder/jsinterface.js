/**
 *    This code will be embedded into your HTML page as "normal"
 *    JavaScript code with a <script> tag. This allows you to
 *    interact with the page as any normal JavaScript code can.
 */
 
 // called once the page has fully loaded
 window.onload = function () {
     makeTheLink();
 }
 
 // make the connection with your sketch
 function makeTheLink() {
     
     // Get the instance. The id is automatically generated 
     // based on the sketch name by removing anything but letters
     // and numbers.
     var mySketchInstance = Processing.getInstanceById( "colorFinder" );
     
     if ( mySketchInstance == undefined ) { // means it has not started
         setTimeout( makeTheLink, 200 );    // try again later
         return;
     }
     
     mySketchInstance.setInterfaceLink(this); // make the connection
 }
 
 // called from your sketch!
 function setColor ( rr, gg, bb ) {
     
     var colorString = 'rgb('+rr+','+gg+','+bb+')';
     
     document.body.style.color = colorString;
     
     var h1s = document.getElementsByTagName("h1");
     if ( h1s != undefined && h1s.length > 0 )
     {
         h1s[0].innerHTML = "Color is: " + colorString;
     }
 }
