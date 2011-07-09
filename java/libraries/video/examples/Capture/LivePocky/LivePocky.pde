/** 
 * Live Pocky
 * by Ben Fry. 
 *
 * GSVideo version by Andres Colubri.  
 * 
 * Unwrap each frame of live video into a single line of pixels.
 */
 
import codeanticode.gsvideo.*;

GSCapture video;
int count;
int writeRow;
int maxRows;
int topRow;
int buffer[];


void setup() {
  size(600, 400);

  // Uses the default video input, see the reference if this causes an error
  video = new GSCapture(this, 160, 120);
  video.start();

  maxRows = height * 2;
  buffer = new int[width * maxRows];
  writeRow = height - 1;
  topRow = 0;
  
  //frameRate(10);
  background(0);
  loadPixels();
}


void draw() {
  for (int y = 0; y < height; y++) {
    int row = (topRow + y) % maxRows;
    arraycopy(buffer, row * width, g.pixels, y*width, width);
  }
  updatePixels();
}


void captureEvent(GSCapture c) {
  c.read();
  c.loadPixels();
  arraycopy(c.pixels, 0, buffer, writeRow * width, width);
  writeRow++;
  if (writeRow == maxRows) {
    writeRow = 0;
  }
  topRow++;
}
