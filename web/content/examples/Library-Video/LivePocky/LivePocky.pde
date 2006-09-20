/** 
 * Live Pocky
 * by Ben Fry. 
 * 
 * Unwrap each frame of live video into a single line of pixels.
 */
 
 
import processing.video.*;

Capture video;
int count;
int writeRow;
int maxRows;
int topRow;
int buffer[];


void setup() {
  size(600, 400, P3D);

  video = new Capture(this, 30, 20); //, 15);
  count = video.width * video.height;

  maxRows = height * 2;
  buffer = new int[width * maxRows];
  writeRow = height - 1;
  topRow = 0;
  frameRate(10);
}


void captureEvent(Capture c) {
  c.read();
  arraycopy(c.pixels, 0, buffer, writeRow * width, width);
  writeRow++;
  if (writeRow == maxRows) writeRow = 0;
  topRow++;
}


void draw() {
  for (int y = 0; y < height; y++) {
    int row = (topRow + y) % maxRows;
    arraycopy(buffer, row * width, g.pixels, y*width, width);
  }
}


void keyPressed() {
  if (key == 'g') saveFrame();
}
