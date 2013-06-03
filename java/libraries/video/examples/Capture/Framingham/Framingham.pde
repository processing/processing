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

// Buffer used to move all the pixels up
int[] scoot;


void setup() {
  size(640, 480);

  // This the default video input, see the GettingStartedCapture 
  // example if it creates an error
  video = new Capture(this, 160, 120);
  
  // Start capturing the images from the camera
  video.start(); 
  
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
    video.loadPixels();
    image(video, video.width*column, video.height*lastRow);
    column++;
    if (column == columnCount) {
      loadPixels();
        
      // Scoot everybody up one row
      arrayCopy(pixels, video.height*width, scoot, 0, scoot.length);
      arrayCopy(scoot, 0, pixels, 0, scoot.length);

      // Set the moved row to black
      for (int i = scoot.length; i < width*height; i++) {
        pixels[i] = #000000;
      }
      column = 0;
      updatePixels();
    }
  }
}
