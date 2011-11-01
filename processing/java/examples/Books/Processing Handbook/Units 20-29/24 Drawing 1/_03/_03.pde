// Draw a line only when a mouse button is pressed
void setup() {
  size(100, 100);
}

void draw() {
  if (mousePressed == true) {
    line(mouseX, mouseY, pmouseX, pmouseY);
  }
}

