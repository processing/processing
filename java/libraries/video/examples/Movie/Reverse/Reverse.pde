/**
 * GSVideo movie reverse example.
 *
 * The GSMovie.speed() method allows to 
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

import codeanticode.gsvideo.*;

GSMovie myMovie;
boolean speedSet = false;

public void setup() {
  size(320, 240);
  background(0);
  myMovie = new GSMovie(this, "balloon.ogg");
  myMovie.play();
}

public void movieEvent(GSMovie myMovie) {
  myMovie.read();  
}

public void draw() {
  if (myMovie.ready()) {
    if (!speedSet) {
      // Setting the speed should be done only once,
      // this is the reason for the if statement.
      speedSet = true;
      myMovie.goToEnd();
      // -1 means backward playback at normal speed,
      myMovie.speed(-1.0);
    }
  }
  image(myMovie, 0, 0, width, height); 
}  
