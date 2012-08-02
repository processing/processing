/**
 *    <video width="160" height="120" style="display:none">
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
     
     noStroke();
     
     textFont(createFont("Arial", 20));
 }
 
 void draw ()
 {
     background(255);
     
     if ( video != null )
     {
         PImage frame = video.getFrame();
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
         
         fill(200);
         rect(5,height-5-20,width-10,20);
         fill(100);
         rect(5,height-5-20,
              map(video.currentTime,0,video.duration,0,width-10),
              20);
     }
 }
 
 boolean wasDragged = false;
 void mouseDragged ()
 {
     if ( video != null )
     {
         // this currently does not work for chrome with our  server
         video.currentTime = map(constrain(mouseX,0,width-10),0,width-10,0,video.duration);
         video.pause();
         wasDragged = true;
     }
 }
 
 void mouseReleased ()
 {
     if ( video != null && !wasDragged )
     {
         if ( video.paused )
             video.play();
         else
             video.pause();
     }
     wasDragged = false;
 }
 
 /* called from JavaScript to set the freshly loaded video */
 void setVideo ( Video v )
 {
     video = v;
     console.log( v );
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
