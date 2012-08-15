/**
 * Frames. 
 * by Andres Colubri
 * 
 * Moves through the video one frame at the time by using the
 * arrow keys. It estimates the frame counts using the framerate
 * of the movie file, so it might not be exact in some cases.
 */
 
import processing.video.*;

Movie movie;
int newFrame;
PFont font;

void setup() {
  size(320, 240, P2D);
  background(0);
  // Load and set the video to play. Setting the video 
  // in play mode is needed so at least one frame is read
  // and we can get duration, size and other information from
  // the video stream. 
  movie = new Movie(this, "station.mov");
  movie.play();
  
  font = loadFont("DejaVuSans-24.vlw");
  textFont(font, 24);
}

void movieEvent(Movie movie) {
  movie.read();  
}

void draw() {
  if (frameCount == 5) {
    // Trick to force start at frame 0...
    newFrame = 0;
    setFrame(newFrame);
  }
  
  image(movie, 0, 0, width, height);
  fill(240, 20, 30);

  text(getFrame() + " / " + (getLength() - 1), 10, 30);  
}

void keyPressed() {
  if (key == CODED) {
    if (keyCode == LEFT) {
      if (0 < newFrame) newFrame--; 
    } else if (keyCode == RIGHT) {
      if (newFrame < getLength() - 1) newFrame++;
    }
  } 
  
  setFrame(newFrame);  
}
  
int getFrame() {    
  return ceil(movie.time() * movie.frameRate) - 1;
}

void setFrame(int n) {
  movie.play();

  float srcFramerate = movie.frameRate;
    
  // The duration of a single frame:
  float frameDuration = 1.0 / srcFramerate;
    
  // We move to the middle of the frame by adding 0.5:
  float where = (n + 0.5) * frameDuration; 
    
  // Taking into account border effects:
  float diff = movie.duration() - where;
  if (diff < 0) {
    where += diff - 0.25 * frameDuration;
  }
    
  movie.jump(where);
  
  movie.pause();  
}  

int getLength() {
  return int(movie.duration() * movie.frameRate);
}  
