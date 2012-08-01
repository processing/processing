// Use translate() to move a shape
void setup() {
  size(100, 100);
  smooth();
  noStroke();
}

void draw() {
  background(126);
  translate(mouseX, mouseY);
  ellipse(0, 0, 33, 33);
}