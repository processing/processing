/**
 * Framingham
 * by Ben Fry.
 *
 * GSVideo version by Andres Colubri.  
 *  
 * Show subsequent frames from video input as a grid. Also fun with movie files.
 */


import codeanticode.gsvideo.*;

GSCapture video;
int column;
int columnCount;
int lastRow;

// Buffer used to move all the pixels up
int[] scoot;


void setup() {
  size(640, 480, P3D);

  // Uses the default video input, see the reference if this causes an error
  video = new GSCapture(this, 160, 120);
  video.start();  
  // Also try with other video sizes
  
  column = 0;
  columnCount = width / video.width;
  int rowCount = height / video.height;
  lastRow = rowCount - 1;
  
  scoot = new int[lastRow*video.height * width];
  background(0);
}


void draw() {
  // By using video.available, only the frame rate need be set inside setup()
  if (video.available()) {
    video.read();
    set(video.width*column, video.height*lastRow, video);
    column++;
    if (column == columnCount) {
      loadPixels();
        
      // Scoot everybody up one row
      arraycopy(pixels, video.height*width, scoot, 0, scoot.length);
      arraycopy(scoot, 0, pixels, 0, scoot.length);

      // Set the moved row to black
      for (int i = scoot.length; i < width*height; i++) {
        pixels[i] = #000000;
      }
      column = 0;
      updatePixels();
    }
  }
}
