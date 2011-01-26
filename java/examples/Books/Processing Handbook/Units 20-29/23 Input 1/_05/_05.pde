// Invert cursor position to create a secondary response
void setup() {
  size(100, 100);
  noStroke();
  smooth();
}

void draw() {
  float x = mouseX;
  float y = mouseY;
  float ix = width - mouseX; // Inverse X
  float iy = mouseY - height; // Inverse Y
  background(126);
  fill(255, 150);
  ellipse(x, height/2, y, y);
  fill(0, 159);
  ellipse(ix, height/2, iy, iy);
}