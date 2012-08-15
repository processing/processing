/**
 * Pixelate  
 * by Hernando Barragan.  
 * 
 * Load a QuickTime file and display the video signal 
 * using rectangles as pixels by reading the values stored 
 * in the current video frame pixels array. 
 */

import processing.video.*;

int numPixels;
int blockSize = 10;
Movie myMovie;
color myMovieColors[];

void setup() {
  size(640, 480, P2D);
  noStroke();
  background(0);
  myMovie = new Movie(this, "station.mov");
  myMovie.loop();
  numPixels = width / blockSize;
  myMovieColors = new color[numPixels * numPixels];
}


// Read new values from movie
void movieEvent(Movie m) {
  m.read();
}


// Display values from movie
void draw()  {
  myMovie.loadPixels();
  
  for (int j = 0; j < numPixels; j++) {
    for (int i = 0; i < numPixels; i++) {
      myMovieColors[j*numPixels + i] = myMovie.get(i, j);
    }
  }
  
  for (int j = 0; j < numPixels; j++) {
    for (int i = 0; i < numPixels; i++) {
      fill(myMovieColors[j*numPixels + i]);
      rect(i*blockSize, j*blockSize, blockSize-1, blockSize-1);
    }
  }
}