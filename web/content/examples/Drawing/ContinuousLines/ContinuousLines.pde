// Continuous Lines
// by REAS <http://reas.com>

// Click and drag the mouse to draw a line.

// Updated 27 October 2002 
// Created 23 October 2002

void setup() {
  size(200, 200);
  background(102);
}

void draw() {
  stroke(255);
  if(mousePressed) {
    line(mouseX, mouseY, pmouseX, pmouseY);
  }
}
