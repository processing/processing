// Draw an ellipse to show the position of the hidden cursor
void setup() {
  size(100, 100);
  strokeWeight(7);
  smooth();
  noCursor();
}

void draw() {
  background(204);
  ellipse(mouseX, mouseY, 10, 10);
}