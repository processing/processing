/**
 *    <video width="160" height="120">
 *        <source src="station.mp4" />
 *        <source src="station.ogv" />
 *        <source src="station.webm" />
 *    </video>
 */
 
 Video video;
 PImage frame;
 
 void setup ()
 {
     size( 320, 240 );
     
     textFont(createFont("Arial", 20));
     noStroke();
     
     textFont(createFont("Arial", 20));
 }
 
 void draw ()
 {
     background(255);
     
     if ( video != null )
     {
         PImage frame = getFrame(video);
         float br = 0;
         color px;
         for ( int ix = 0; ix < frame.width; ix += 7 )
         {
             for ( int iy = 0; iy < frame.height; iy += 7 )
             {
                 px = frame.pixels[ ix + iy * frame.width ];
                 fill(px);
                 br = brightness(px);
                 br = map(br,0,255,1,14);
                 ellipse(ix*2, iy*2, br, br);
             }
         }
         fill(0);
         text(video.currentTime, 5, height-5);
     }
 }
 
 void mouseReleased ()
 {
     if ( video != null )
     {
         if ( video.paused || video.ended )
         {
             video.loop = true;
             video.play();
         }
         else
             video.pause();
     }
 }
 
 void mouseDragged ()
 {
     if ( video != null )
     {
         video.pause();
         video.currentTime = map(mouseX,0,width,0,video.duration);
     }
 }
 
 /* called from JavaScript to set the freshly loaded video */
 void setVideo ( Video v )
 {
     video = v;
 }
 
 /* copy video image to PImage */
 PImage getFrame ( Video v )
 {
     return new PImage(v); // sub-optimal ..
 }
 
 /* make Processing understand the HTMLVideoElement */
 interface Video
 {
     boolean autoplay;
     boolean controls;
     
     int width;
     int height;
     int videoWidth; /*readonly*/
     int videoHeight; /*readonly*/
     
     boolean muted;
     float volume;
     
     boolean loop;
     boolean paused;
     boolean ended;
     
     String currentSrc;
     
     float duration;
     float currentTime;
     
     void play();
     void pause();
 }
