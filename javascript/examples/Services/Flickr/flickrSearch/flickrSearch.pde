/**
 *  A simple Flickr search.<br />
 *
 *  <form>
 *    <input id="form-input" type="text" value="processing.org" />
 *    <input id="form-submit" type="submit" value="search" />
 *  </form>
 */
 
 HashMap<Integer,FImage> fImages;
 HashMap<Integer,PImage> images;
 
 int scrollY=0;
 int w75;
 
 void setup ()
 {
     size( 600, 300 );
     
     w75 = width/75;
     
     textFont(createFont("Helvetica", 10));
 }
 
 void draw ()
 {
     background( 255 );
     
     if ( images != null && images.size() > 0 )
     {
         translate(0,scrollY);
         
         Iterator imgs = images.values().iterator();
         int i = 0;
         while ( imgs.hasNext() )
         {
             PImage img = imgs.next();
             image(img, int(i%w75)*75, int(i/w75)*75);
             i++;
         }
     }
     else
     {
         translate( width/2, height/2 );
         noStroke();
         for ( float a = 0, r = 0; a < 360; a+=24 )
         {
             fill( map( int(a+frameCount)%360, 0, 360, 0, 255 ) );
             r = radians(a);
             ellipse( sin(r)*20, cos(r)*20, 5, 5 );
         }
     }
 }
 
 int pressedY; int pScrollY;
 void mousePressed ()
 {
     pressedY = mouseY;
     pScrollY = scrollY;
 }
 
 void mouseDragged ()
 {
     if ( images != null && images.size() > 0 )
     {
         int newScrollY = pScrollY + (mouseY-pressedY);
         if ( newScrollY <= 0 && newScrollY > -int(images.size()/w75)*75 + height )
         {
             scrollY = newScrollY;
         }
     }
 }
 
 void resetFlickrImages ()
 {
     if ( images != null )
         for ( PImage i : images.values() ) i = null;
     images = null;
     images = new HashMap<Integer,PImage>();
     
     if ( fImages != null )
         for ( FImage i : fImages.values() ) i = null;
     fImages = null;
     fImages = new HashMap<Integer,FImage>();
     
     scrollY = 0;
 }
 
 void newFlickrImage ( FImage fi )
 {
     images.put( int(fi.id), loadImage(fi.url_sq) );
     fImages.put( int(fi.id), fi );
 }
 
 interface FImage
 {
     String id;
     
     String owner;
     String owner_name;
     String title;
     
     String url_sq;
     String height_sq;
     String width_sq;
     
     String url_t;
     String height_t;
     String width_t;
     
     String url_s;
     String height_s;
     String width_s;
 }
