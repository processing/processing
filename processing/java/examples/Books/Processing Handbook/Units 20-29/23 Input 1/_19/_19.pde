// Hides the cursor until a mouse button is pressed
void setup() {
  size(100, 100);
  noCursor();
}

void draw() {
  background(204);
  if (mousePressed == true) {
    cursor();
  }
}