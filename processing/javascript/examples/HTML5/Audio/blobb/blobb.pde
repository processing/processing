/**
 *   Blobbbs. Bouncy, noisy.<br />
 *
 *   <audio>
 *    <source src="BD.wav" />
 *    <source src="BD.mp3" />
 *  </audio>
 */
 
 Ball[] balls;
 float maxSpeed = 0.5;
 Audio audio;
 
 void setup ()
 {
     size( 400, 300 );
 }
 
 void draw ()
 {
     background(75);
     
     if ( balls != null )
     {
         for ( int i = 0; i < balls.length; i++ )
         {
             balls[i].update();
             balls[i].draw();
         }
     }
 }
 
 // a ball class .. again.
 class Ball
 {
     float px,py;
     float dx,dy;
     float rad;
     float col;
     float hitness;
     
     Ball ()
     {
         rad = random(2,20);
         
         px = rad + random(width-2*rad);
         py = rad + random(height-2*rad);
         dx = random(-maxSpeed,maxSpeed); 
         dy = random(-maxSpeed,maxSpeed);
         
         colorMode(HSB);
         col = color(random(255),200,150);
         colorMode(RGB);
         
         hitness = 50;
     }
     
     void update ()
     {
         px += dx;
         py += dy;
         
         hitness -= hitness/10;
         
         if ( px-rad <= 0 || px+rad >= width  )
         {
             dx *= -1;
             playSound();
             hitness = 10;
         }
         if ( py-rad <= 0 || py+rad >= height )
         {
             dy *= -1;
             playSound();
             hitness = 10;
         }
     }
     
     void playSound ()
     {
         if ( audio != null )
         {
             audio.pause();
             audio.currentTime = 0;
             audio.play();
         }
     }
     
     void draw ()
     {
         strokeWeight( hitness );
         stroke(200);
         fill(col);
         ellipse( px, py, rad*2, rad*2 );
     }
 }
 
 // called from JavaScript to set the Audio Element
 void newAudio ( Audio a )
 {
     if ( audio == null )
     {
         audio = a;
         
         balls = new Ball[10];
         for ( int i = 0; i < balls.length; i++ )
         {
             balls[i] = new Ball();
         }
     }
 }
 
 // let Processing know about the HTMLAudioElement:
 // http://dev.w3.org/html5/spec/the-iframe-element.html#the-audio-element
 interface Audio
 {
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
     
     Audio cloneNode( boolean deep ); /*DOM*/
 }
