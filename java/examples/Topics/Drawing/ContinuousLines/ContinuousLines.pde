/**
 * Continuous Lines. 
 * 
 * Click and drag the mouse to draw a line. 
 */

void setup() {
  size(640, 360);
  background(102);
}

void draw() {
  stroke(255);
  if (mousePressed == true) {
    line(mouseX, mouseY, pmouseX, pmouseY);
  }
}
