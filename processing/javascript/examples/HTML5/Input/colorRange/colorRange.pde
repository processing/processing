/**
 *    This examples shows you how to interact with the HTML5 range input. <br />
 *
 *    <form>
 *        <input id="form-range" type="range" min="0" max="100" value="0" />
 *        <!-- try http://frankyan.com/labs/html5slider/ for FireFox support -->
 *    </form><br />
 *    (Slider by Firefox currently not supported.) <br />
 */
 
 float rangeValue = 0;
 float gColorValue = 0;
 
 void setup ()
 {
     size(300,200);
     colorMode(HSB);
 }
 
 void draw ()
 {
     background(gColorValue, 200, 120);
     
     float c = int(map( rangeValue, 0, 100, 0, 255 ));
     
     fill( c, 200, 150 );
     stroke( c, 180, 100 );
     
     float x = map( rangeValue, 1, 100, 0, width );
     
     ellipse( x, height/2, 50, 50 );
     
     gColorValue += (c-gColorValue)/30.0;
 }
 
 /* this is being called from JavaScript when the range slider is changed */
 void newRangeValue ( float v )
 {
     rangeValue = v;
 }
