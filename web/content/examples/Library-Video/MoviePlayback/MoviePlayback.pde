// Movie Playback
// by hbarragan

// Move the cursor across the screen to draw.
// Shows how to load and play a QuickTime movie file.

// Created 21 June 2003
// Updated 30 March 2005 by REAS

import processing.video.*;
Movie myMovie;

void setup() 
{
  size(200, 200);
  background(0);
  // Load and play the video in a loop
  myMovie = new Movie(this, "station.mov");
  myMovie.loop();
}

void movieEvent(Movie myMovie) {
  myMovie.read();
}


void draw() 
{
  tint(255, 20);
  image(myMovie, mouseX-myMovie.width/2, mouseY-myMovie.height/2);
}
