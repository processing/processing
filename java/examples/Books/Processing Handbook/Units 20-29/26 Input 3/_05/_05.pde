void setup() {
  size(100, 100);
  noStroke();
  fill(255, 51);
}

void draw() { } // Empty draw() keeps the program running

void keyPressed() {
  int y = key - 32;
  rect(0, y, 100, 4);
}