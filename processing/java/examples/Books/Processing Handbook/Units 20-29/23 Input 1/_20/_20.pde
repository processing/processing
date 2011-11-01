// Draws the cursor as a hand when a mouse button is pressed
void setup() {
  size(100, 100);
  smooth();
}

void draw() {
  background(204);
  if (mousePressed == true) {
    cursor(HAND);
  } else {
    cursor(MOVE);
  }
  line(mouseX, 0, mouseX, height);
  line(0, mouseY, height, mouseY);
}