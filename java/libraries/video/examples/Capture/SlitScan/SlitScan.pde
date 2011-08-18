/**
 * Simple Real-Time Slit-Scan Program. 
 * By Golan Levin.
 *
 * GSVideo version by Andres Colubri. 
 * 
 * This demonstration depends on the canvas height being equal 
 * to the video capture height. If you would prefer otherwise, 
 * consider using the image copy() function rather than the 
 * direct pixel-accessing approach I have used here. 
 * 
 * Created December 2006. 
 * Updated June 2007 by fry.
 */
import processing.video.*;

Capture video;

int videoSliceX;
int drawPositionX;

void setup() {
  size(600, 240);
  
  // Uses the default video input, see the reference if this causes an error
  video = new Capture(this, 320, 240);
  video.start();
  
  videoSliceX = video.width / 2;
  drawPositionX = width - 1;
  background(0);
}


void draw() {
  if (video.available()) {
    video.read();
    video.loadPixels();
    
    // Copy a column of pixels from the middle of the video 
    // To a location moving slowly across the canvas.
    loadPixels();
    for (int y = 0; y < video.height; y++){
      int setPixelIndex = y*width + drawPositionX;
      int getPixelIndex = y*video.width  + videoSliceX;
      pixels[setPixelIndex] = video.pixels[getPixelIndex];
    }
    updatePixels();
    
    drawPositionX--;
    // Wrap the position back to the beginning if necessary.
    if (drawPositionX < 0) {
      drawPositionX = width - 1;
    }
  }
}
