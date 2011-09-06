/**
 * Continuous Lines. 
 * 
 * Click and drag the mouse to draw a line. 
 */
 
int px, py;

void setup() {
  size(640, 360);
  background(102);
}

void draw() {
  stroke(255);
  if(mousePressed) {
    line(mouseX, mouseY, px, py);
  }
  px = mouseX;
  py = mouseY;
}
