/**
 * Pixelate  
 * by Hernando Barragan.  
 * Built-in video library replaced with gsvideo by Andres Colubri
 * 
 * Load a QuickTime file and display the video signal 
 * using rectangles as pixels by reading the values stored 
 * in the current video frame pixels array. 
 */

import codeanticode.gsvideo.*;

int numPixels;
int blockSize = 10;
GSMovie myMovie;
color myMovieColors[];

void setup() {
  size(640, 480);
  noStroke();
  background(0);
  myMovie = new GSMovie(this, "station.mov");
  myMovie.loop();
  numPixels = width / blockSize;
  myMovieColors = new color[numPixels * numPixels];
}


// Read new values from movie
void movieEvent(GSMovie m) {
  m.read();
  m.loadPixels();
  
  for (int j = 0; j < numPixels; j++) {
    for (int i = 0; i < numPixels; i++) {
      myMovieColors[j*numPixels + i] = m.get(i, j);
    }
  }
}


// Display values from movie
void draw()  {
  for (int j = 0; j < numPixels; j++) {
    for (int i = 0; i < numPixels; i++) {
      fill(myMovieColors[j*numPixels + i]);
      rect(i*blockSize, j*blockSize, blockSize-1, blockSize-1);
    }
  }
}
