/**
 *    Select an image to have it be transferred to your sketch
 *    without the need to be uploaded first. <br />
 *
 *    <form><input type="file" id="file-input"/></form>
 *
 *    No Safari support.
 */
 
 PImage img = null;
 
 void setup ()
 {
     size(300, 200);
 }
 
 void draw ()
 {
     background(255);
     if ( img != null ) image(img, 0,0, width,height);
 }
 
 void newImageAvailable ( Image i )
 {
     img = new PImage( i );
 }
