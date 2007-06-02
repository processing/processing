/** 
 * Multiple Frames. 
 * 
 * Saves one PDF document of many frames drawn to the screen.
 * Starts the file when the mouse is pressed and end the file
 * when the mouse is released.
 */


import processing.pdf.*;

void setup() {
  size(600, 600);
  frameRate(24);
  background(255);
}

void draw() {
  stroke(0, 20);
  strokeWeight(20.0);
  line(mouseX, 0, width-mouseY, height);
}

void mousePressed() {
  beginRecord(PDF, "Lines.pdf"); 
  background(255);
}

void mouseReleased() {
  endRecord();
  background(255);
}

