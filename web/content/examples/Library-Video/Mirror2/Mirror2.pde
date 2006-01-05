// Mirror1
// Daniel Shiffman <http://www.shiffman.net>

// Each pixel from the video source is drawn as a 
// rectangle with size based on brightness. 

// Created 2 May 2005

import processing.video.*;

// Size of each cell in the grid
int cellsize = 10;
// Number of columns and rows in our system
int cols, rows;
// Variable for capture device
Capture video;


void setup()
{
  size(200, 200);
  framerate(30);
  //set up columns and rows
  cols = width/cellsize;
  rows = height/cellsize;
  colorMode(RGB,255,255,255,100);
  smooth();

  // Using the default capture device
  video = new Capture(this, 200, 200, 15);

  background(0);

}

void captureEvent(Capture camera)
{
  camera.read();
}

void draw()
{ 
  background(0);

  // Begin loop for columns
  for ( int i = 0; i < cols;i++) {
    // Begin loop for rows
    for ( int j = 0; j < rows;j++) {

      // Where are we, pixel-wise?
      int x = i*cellsize;
      int y = j*cellsize;
      int loc = (video.width - x - 1) + y*video.width; // Reversing x to mirror the image

      // Each rect is colored white with a size determined by brightness
      color c = video.pixels[loc];
      float sz = (brightness(c)/255.0f)*cellsize;
      rectMode(CENTER);
      fill(255);
      noStroke();
      rect(x+cellsize/2,y+cellsize/2,sz,sz);
    }
  }
}




