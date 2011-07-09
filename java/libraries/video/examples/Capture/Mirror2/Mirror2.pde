/**
 * Mirror 2 
 * by Daniel Shiffman. 
 *
 * GSVideo version by Andres Colubri.  
 * 
 * Each pixel from the video source is drawn as a rectangle with size based on brightness.  
 */
 
import codeanticode.gsvideo.*;

// Size of each cell in the grid
int cellSize = 15;
// Number of columns and rows in our system
int cols, rows;
// Variable for capture device
GSCapture video;


void setup() {
  size(640, 480, P3D);
  //set up columns and rows
  cols = width / cellSize;
  rows = height / cellSize;
  colorMode(RGB, 255, 255, 255, 100);
  rectMode(CENTER);

  // Uses the default video input, see the reference if this causes an error
  video = new GSCapture(this, width, height);
  video.start();

  background(0);
}


void draw() { 
  if (video.available()) {
    video.read();
    video.loadPixels();
    
    background(0, 0, 255);

    // Begin loop for columns
    for (int i = 0; i < cols;i++) {
      // Begin loop for rows
      for (int j = 0; j < rows;j++) {

        // Where are we, pixel-wise?
        int x = i * cellSize;
        int y = j * cellSize;
        int loc = (video.width - x - 1) + y*video.width; // Reversing x to mirror the image

        // Each rect is colored white with a size determined by brightness
        color c = video.pixels[loc];
        float sz = (brightness(c) / 255.0) * cellSize;
        fill(255);
        noStroke();
        rect(x + cellSize/2, y + cellSize/2, sz, sz);
      }
    }
  }
}
