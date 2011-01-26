void setup() {
  size(100, 100);
  fill(0, 102);
}

void draw() { } // Empty draw() keeps the program running

void mousePressed() {
  rect(mouseX, mouseY, 33, 33);
}