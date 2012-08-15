/**
 * Reverse playback example.
 *
 * The Movie.speed() method allows to 
 * change the playback speed. Use negative
 * values for backwards playback. Note that
 * not all video formats support backwards
 * playback. This depends on the underlying
 * gstreamer plugins used by gsvideo. For
 * example, the theora codec does support 
 * backward playback, but not so the H264 
 * codec, at least in its current version.
 * 
 */

import processing.video.*;

Movie myMovie;
boolean speedSet = false;

void setup() {
  size(320, 240, P2D);
  background(0);
  myMovie = new Movie(this, "balloon.ogg");
  myMovie.play();
}

void movieEvent(Movie myMovie) {
  myMovie.read();  
}

void draw() {
  if (!speedSet) {
    // Setting the speed should be done only once,
    // this is the reason for the if statement.
    speedSet = true;
    myMovie.jump(myMovie.duration());
    // -1 means backward playback at normal speed.
    myMovie.speed(-1.0);
    // Setting to play again, since the movie stop
    // playback once it reached the end.
    myMovie.play();
  }
  image(myMovie, 0, 0, width, height); 
}   