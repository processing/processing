// Simple Color Tracking
// Daniel Shiffman <http://www.shiffman.net>

// Select a color to track by clicking the mouse

// Created 2 May 2005

import processing.video.*;

// Variable for capture device
Capture video;
color trackColor;


void setup()
{
  size(200, 200);
  framerate(30);
  colorMode(RGB,255,255,255,100);
  // Using the default capture device
  video = new Capture(this, 200, 200, 12);
  trackColor = color(255); // Start off tracking for white
  noFill();
  smooth();
  strokeWeight(4.0);
  stroke(0);

}

void captureEvent(Capture camera)
{
  camera.read();
}

void draw()
{

  loadPixels();
  
  // Draw the video image on the background
  image(video,0,0);
  // Local variables to track the color
  float closestDiff = 500.0f;
  int closestX = 0;
  int closestY = 0;
  // Begin loop to walk through every pixel
  for ( int x = 0; x < video.width; x++) {
    for ( int y = 0; y < video.height; y++) {
      int loc = x + y*video.width;
      // What is current color
      color currentColor = video.pixels[loc];
      float r1 = red(currentColor); float g1 = green(currentColor); float b1 = blue(currentColor);
      float r2 = red(trackColor);   float g2 = green(trackColor);   float b2 = blue(trackColor);
      // Using euclidean distance to compare colors
      float d = dist(r1,g1,b1,r2,g2,b2); 
      // If current color is more similar to tracked color than
      // closest color, save current location and current difference
      if (d < closestDiff) {
        closestDiff = d;
        closestX = x;
        closestY = y;
      }
    }
  }
  // Draw a circle at the tracked pixel
  ellipse(closestX,closestY,16,16);
}

void mousePressed() {
  // Save color where the mouse is clicked in trackColor variable
  int loc = mouseX + mouseY*video.width;
  trackColor = video.pixels[loc];
}



