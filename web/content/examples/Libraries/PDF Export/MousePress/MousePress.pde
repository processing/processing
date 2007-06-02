 /**
 * Mouse Press. 
 * 
 * Saves one PDF of the contents of the display window 
 * each time the mouse is pressed.
 */


import processing.pdf.*;

boolean saveOneFrame = false;

void setup() {
  size(600, 600);
  frameRate(24);
}

void draw() {
  if(saveOneFrame == true) {
    beginRecord(PDF, "Line.pdf"); 
  }
  
  background(255);
  stroke(0, 20);
  strokeWeight(20.0);
  line(mouseX, 0, width-mouseY, height);
  
  if(saveOneFrame == true) {
    endRecord();
    saveOneFrame = false; 
  }
}

void mousePressed() {
  saveOneFrame = true; 
}

