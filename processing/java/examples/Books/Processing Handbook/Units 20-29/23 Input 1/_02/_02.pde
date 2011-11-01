// Circle follows the cursor
void setup() {
  size(100, 100);
  smooth();
  noStroke();
}

void draw() {
  background(126);
  ellipse(mouseX, mouseY, 33, 33);
}