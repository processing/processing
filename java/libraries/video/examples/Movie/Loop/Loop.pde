/**
 * Loop. 
 * Built-in video library replaced with gsvideo by Andres Colubri
 * 
 * Move the cursor across the screen to draw. 
 * Shows how to load and play a QuickTime movie file.  
 *
 * Note: GSVideo uses GStreamer as the underlying multimedia library
 * for reading media files, decoding, encoding, etc.
 * It is based on a set of Java bindings for GStreamer called
 * gstreamer-java originally created by Wayne Meissner and currently
 * mantained by a small team of volunteers. GStreamer-java can be 
 * used from any Java program, and it is available for download at
 * the following website:
 * http://code.google.com/p/gstreamer-java/
 */

import codeanticode.gsvideo.*;

GSMovie movie;

void setup() {
  size(640, 480);
  background(0);
  // Load and play the video in a loop
  movie = new GSMovie(this, "station.mov");
  movie.loop();
}

void movieEvent(GSMovie movie) {
  movie.read();
}

void draw() {
  tint(255, 20);
  image(movie, mouseX-movie.width/2, mouseY-movie.height/2);
}
