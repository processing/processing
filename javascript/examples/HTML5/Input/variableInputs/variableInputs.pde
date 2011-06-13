/**
 *    This examples shows you how to interact with diverse HTML inputs. It follows 
 *    roughly the way that <a href="http://www.sojamo.de/libraries/controlP5/">ControlP5</a> 
 *    works for standard Processing. <br />
 *
 *    <form id="form-form"><!-- empty --></form>
 *    <!-- the following css adds a tiny bit of layout -->
 *    <style>textarea,input,label,select{display:block;width:95%}select{width:97.5%}
 *    input[type=checkbox],input[type=radio]{width: auto}textarea{height:5em}</style>
 */
 
 /**
  *    This example is rather complicated. You can think of it having two parts:
  *    1) the interface "stuff": setting it up, handle events
  *    2) the layout based upon the values received from inputs
  */
 
 String[] menuItems;
 
 // dafault input values
 int currentShape = 2;
 float currentX = 0;
 boolean hasStroke = true;
 float hueValue = 0;
 String fieldString = "Fancy Corp. Co.";
 String areaString = "We are the fresh new company with "+
                     "activities ranging from A to Z and from "+
                     "alpha to omega.";
 
 void setup ()
 {
     size(300,200);
     
     colorMode(HSB);
     
     currentX = 50;
     
     menuItems = new String[] {
         new String[] {"Rectangle"}, new String[] {"Ellipse"},
         new String[] {"Star"}, new String[] {"Spirograph"}
     };
     
     textFont(createFont("Arial", 16));
 }
 
 /* drawing the layout */
 
 void draw ()
 {
     background( 200 );
     
     strokeWeight(4);
     
     if ( hasStroke ) stroke( hueValue, 150, 95 );
     else noStroke();
     
     fill( hueValue, 200, 150 );
     
     pushMatrix();
     switch ( currentShape ) {
         case 0:
             rectMode(CENTER);
             rect(currentX, height/4, 50, 50);
             break;
         case 1:
             ellipse(currentX, height/4, 55, 55);
             break;
         case 2:
             star(currentX, height/4, 17, 30);
             break;
         case 3:
             spiro(currentX, height/4, 20);
             break;
     }
     popMatrix();
     
     fill( 0 );
     textSize(16);
     textAlign( CENTER );
     float tWidth = textWidth(fieldString);
     float tX = currentX;
     if ( currentX-tWidth/2 < 25 )
     {
         textAlign( LEFT );
         tX = currentX-25;
     }
     else if ( currentX+tWidth/2 > width-25 )
     {
         textAlign( RIGHT );
         tX = currentX+25;
     }
     text( fieldString, tX, height/4+50 );
     
     textSize(11.5);
     textAlign( currentX > width/2 ? RIGHT : LEFT );
     int l, w;
     if ( currentX <= width/2 )
     {
         l = currentX-50+25;
         w = width-l-25; 
     }
     else
     {
         l = 25;
         w = currentX+50-25-25; 
     }
     text( areaString, l, height/4+70, w, height/2 );
 }
 
 void star ( float x, float y, float inner, float outer )
 {
     beginShape();
     for ( int i = 0; i < 360; i+=36 )
     {
         float r = radians(i + sin(frameCount/90.0)*25);
         vertex( x + cos(r)*outer, y + sin(r)*outer );
         r = radians(i+(36/2));
         vertex( x + cos(r)*inner, y + sin(r)*inner );
     }
     endShape(CLOSE);
 }
 
 void spiro ( float x, float y, float rad )
 {
     beginShape();
     for ( int i = 0; i < 360; i+=2 )
     {
         float r = radians(i);
         float r2 = radians(i*(sin(frameCount/240.0)+2)*2);
         vertex( x + (cos(r)+cos(r2)/2)*rad, y + (sin(r)+sin(r2)/2)*rad );
     }
     endShape();
 }
 
 /* interface related things */
 
 void setController ( Controller ctlr )
 {
     // labels are supposed to be existing function names
     
     InterfaceElement element = ctlr.addRange( "rangeCallback", currentX, 0, 100 );
     ctlr.setElementLabel( element, "Example range input field" );
     
     element = ctlr.addCheckbox( "textBoxCallback", hasStroke );
     ctlr.setElementLabel( element, "A checkbox here" );
     
     element = ctlr.addTextfield( "textFieldChanged", fieldString );
     ctlr.setElementLabel( element, "... and this is a textfield" );
     
     element = ctlr.addTextarea( "calledByTextarea", areaString );
     ctlr.setElementLabel( element, "Ta-dah: a textarea" );
     
     element = ctlr.addMenu( "theMenu", menuItems );
     ctlr.setElementLabel( element, "LBNL a select menu" );
 }
 
 /* callbacks */
 
 void rangeCallback ( float value )
 {
     currentX = map( value, 0, 100, 50, width-50 );
 }
 
 void textBoxCallback ( boolean value )
 {
     hasStroke = value;
 }
 
 void textFieldChanged ( String value )
 {
     fieldString = value;
 }
 
 void calledByTextarea ( String value )
 {
     areaString = value;
 }
 
 void theMenu ( String value )
 {
     currentShape = int(value);
 }
 
 /* ... and the interfaces */
 
 /* explain InputElement to Processing */
 interface InputElement
 {
     String type;
     String id;
     Object value;
 }
 
 /* explain Controller to Processing */
 interface Controller
 {
     InputElement addRange ( String label, float initialValue, float minValue, float maxValue );
     void setLabel ( InputElement element, String label );
 }
