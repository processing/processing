/**
 * Drawing Movie. 
 * 
 * Makes a QuickTime movie of a line drawn by the mouse. Press
 * the spacebar to finish and save the movie. 
 */

import processing.video.*;

MovieMaker mm;

void setup() {
  size(320, 240);

  // Save uncompressed, at 15 frames per second
  mm = new MovieMaker(this, width, height, "drawing.mov");
  
  // Or, set specific compression and frame rate options
  //mm = new MovieMaker(this, width, height, "drawing.mov", 30, 
  //                    MovieMaker.ANIMATION, MovieMaker.HIGH);
  
  background(160, 32, 32);
}


void draw() {
  stroke(7, 146, 168);
  strokeWeight(4);

  // Draw if mouse is pressed
  if (mousePressed && pmouseX != 0 && mouseY != 0) {
    line(pmouseX, pmouseY, mouseX, mouseY);
  }

  // Add window's pixels to movie
  mm.addFrame();
}


void keyPressed() {
  if (key == ' ') {
    // Finish the movie if space bar is pressed
    mm.finish();
    // Quit running the sketch once the file is written
    exit();
  }
}

