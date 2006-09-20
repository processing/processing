/**
 * Framingham
 * by Ben Fry.
 *  
 * Show subsequent frames from video input as a grid. Also fun with movie files.
 */

import processing.video.*;

Capture video;
int column;
int columnCount;
int lastRow;

// buffer used to move all the pixels up
int scoot[];


void setup() {
  size(640, 480, P3D);

  // also try with other video sizes
  video = new Capture(this, 32, 24);
  
  column = 0;
  columnCount = width / video.width;
  int rowCount = height / video.height;
  lastRow = rowCount - 1;
  
  scoot = new int[lastRow*video.height * width];
  background(0);
  frameRate(10);  // try different frame rates for different effects
}


void draw() {
  // by using video.available, only the frame rate need be set inside setup()
  if (video.available()) {
    video.read();
    set(video.width*column, video.height*lastRow, video);
    column++;
    if (column == columnCount) {
      loadPixels();
        
      // scoot everybody up one row
      arraycopy(pixels, video.height*width, scoot, 0, scoot.length);
      arraycopy(scoot, 0, pixels, 0, scoot.length);

      // set the moved row to black
      for (int i = scoot.length; i < width*height; i++) {
        pixels[i] = #000000;
      }
      column = 0;
      updatePixels();
    }
  }
}
