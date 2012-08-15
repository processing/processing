/**
 * Scratch. 
 * by Andres Colubri
 * 
 * Move the cursor horizontally across the screen to set  
 * the position in the movie file.
 */
 
import processing.video.*;

Movie movie;

void setup() {
  size(640, 480, P2D);
  background(0);

  movie = new Movie(this, "station.mov");

  // Pausing the video at the first frame. 
  movie.play();
  movie.jump(0);
  movie.pause();
}

void movieEvent(Movie movie) {
  movie.read();
}

void draw() {
  // A new time position is calculated using the current mouse location:
  float f = constrain((float)mouseX / width, 0, 1);
  float t = movie.duration() * f;    
  movie.play();
  movie.jump(t);
  movie.pause();   
  image(movie, 0, 0, width, height);
}