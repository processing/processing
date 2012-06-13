/**
 * GSVideo movie speed example.
 *
 * Use the Movie.speed() method to change
 * the playback speed.
 * 
 */

import processing.video.*;

Movie movie;

void setup() {
  size(320, 240);
  background(0);
  movie = new Movie(this, "balloon.ogg");
  movie.loop();
  
  PFont font = loadFont("DejaVuSans-24.vlw");
  textFont(font, 24);
}

void movieEvent(Movie movie) {
  movie.read();  
}

void draw() {    
  image(movie, 0, 0, width, height);
    
  float newSpeed = map(mouseX, 0, width, 0.1, 2);
  movie.speed(newSpeed);
  fill(240, 20, 30);
  text(nfc(newSpeed, 2) + "X", width - 80, 30); 
}  

