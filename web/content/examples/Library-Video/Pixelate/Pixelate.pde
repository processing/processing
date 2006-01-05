// Pixelate a movie file
// by hbarragan

// Load a QuickTime m file and display the video signal 
// using rectangles as pixels by reading the values stored 
// in the current video frame pixels array

// Created 21 June 2003
// Updated 31 March 2005 by REAS

import processing.video.*;

int numPixels;
int blockSize = 10;
Movie myMovie;
color myMovieColors[];

void setup() 
{
  size(200, 200);
  noStroke();
  background(0);
  myMovie = new Movie(this, "station.mov");
  myMovie.loop();
  numPixels = width / blockSize;
  myMovieColors = new color[numPixels * numPixels];
}

// Read new values from movie
void movieEvent(Movie myMovie) 
{
  myMovie.read();
  for(int j=0; j<numPixels; j++) {
    for(int i=0; i<numPixels; i++) {
      myMovieColors[j*numPixels + i] = myMovie.get(i, j);
    }
  }
}

// Display values from movie
void draw() 
{
  for(int j=0; j<numPixels; j++) {
    for(int i=0; i<numPixels; i++) {
      fill(myMovieColors[j*numPixels + i]);
      rect(i*blockSize, j*blockSize, blockSize-1, blockSize-1);
    }
  }
}


