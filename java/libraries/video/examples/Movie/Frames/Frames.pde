/**
 * Frames. 
 * by Andres Colubri
 * 
 * Moves through the video one frame at the time by using the
 * arrow keys.
 */
 
import processing.video.*;

Movie movie;
int newFrame = 0;
PFont font;

void setup() {
  size(320, 240);
  background(0);
  // Load and set the video to play. Setting the video 
  // in play mode is needed so at least one frame is read
  // and we can get duration, size and other information from
  // the video stream. 
  movie = new Movie(this, "station.mov");
  
  // Pausing the video at the first frame. 
  movie.play();
  movie.goToBeginning();
  movie.pause();
  
  font = loadFont("DejaVuSans-24.vlw");
  textFont(font, 24);
}

void movieEvent(Movie movie) {
  movie.read();
}

void draw() {
  image(movie, 0, 0, width, height);
  fill(240, 20, 30);

  text(movie.frame() + " / " + (movie.length() - 1), 10, 30);
}

void keyPressed() {
  if (key == CODED) {
    if (keyCode == LEFT) {
      if (0 < newFrame) newFrame--; 
    } else if (keyCode == RIGHT) {
      if (newFrame < movie.length() - 1) newFrame++;
    }
  } 
  
  movie.play();
  movie.jump(newFrame);
  movie.pause();  
}
