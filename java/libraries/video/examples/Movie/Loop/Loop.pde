/**
 * Loop. 
 * 
 * Move the cursor across the screen to draw. 
 * Shows how to load and play a QuickTime movie file.  
 *
 */

import processing.video.*;

Movie movie;

void setup() {
  size(640, 480, P2D);
  background(0);
  // Load and play the video in a loop
  movie = new Movie(this, "station.mov");
  movie.loop();
}

void movieEvent(Movie movie) {
  movie.read();
}

void draw() {
  tint(255, 20);
  image(movie, mouseX-movie.width/2, mouseY-movie.height/2);
}
