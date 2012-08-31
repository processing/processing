/**
 * Scratch. 
 * by Andres Colubri
 * 
 * Move the cursor horizontally across the screen to set  
 * the position in the movie file.
 */
 
import processing.video.*;

Movie mov;

void setup() {
  size(640, 360);
  background(0);

  mov = new Movie(this, "transit.mov");

  // Pausing the video at the first frame. 
  mov.play();
  mov.jump(0);
  mov.pause();
}

void movieEvent(Movie m) {
  m.read();
}

void draw() {
  // A new time position is calculated using the current mouse location:
  float f = constrain((float)mouseX / width, 0, 1);
  float t = mov.duration() * f;    
  if (mov.ready()) {
    mov.play();
    mov.jump(t);
    mov.pause();
  }    
  image(mov, 0, 0);
}

