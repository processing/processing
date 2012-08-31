/**
 * Scratch 
 * by Andres Colubri. 
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

void draw() {

  if (mov.available()) {
    mov.read();
    // A new time position is calculated using the current mouse location:
    float f = map(mouseX, 0, width, 0, 1);
    float t = mov.duration() * f;
    mov.play();
    mov.jump(t);
    mov.pause();
  }  

  image(mov, 0, 0);
}

