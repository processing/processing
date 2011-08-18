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
  size(640, 480);
  background(0);
  // Load and set the video to play. Setting the video 
  // in play mode is needed so at least one frame is read
  // and we can get duration, size and other information from
  // the video stream. 
  movie = new Movie(this, "station.mov");
  movie.play();
}

void movieEvent(Movie movie) {
  movie.read();
}

void draw() {
  // A new time position is calculated using the current mouse location:
  float f = constrain((float)mouseX / width, 0, 1);
  float t = movie.duration() * f;
    
  // If the new time is different enough from the current position,
  // then we jump to the new position. But how different? Here the
  // difference has been set to 0.1 (1 tenth of a second), but it can
  // be smaller. My guess is that the smallest value should correspond
  // to the duration of a single frame (for instance 1/24 if the frame rate 
  // of the video file is 24fps). Setting even smaller values seem to lead
  // to choppiness. This will become trickier once the Movie.speed()  
  // and Movie.frameRate() methods become functional. 
  if (0.1 < abs(t - movie.time())) {
    // The movie stream must be in play mode in order to jump to another
    // position along the stream. Otherwise it won't work.
    movie.play();
    movie.jump(t);
    movie.pause();
  }
  image(movie, 0, 0, width, height);
}
