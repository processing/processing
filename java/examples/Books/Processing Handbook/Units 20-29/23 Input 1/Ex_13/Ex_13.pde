// Cursor position selects a quadrant of
// the display window
void setup() {
  size(100, 100);
  noStroke();
  fill(0);
}

void draw() {
  background(204);
  if ((mouseX <= 50) && (mouseY <= 50)) {
    rect(0, 0, 50, 50); // Upper-left
  } else if ((mouseX <= 50) && (mouseY > 50)) {
    rect(0, 50, 50, 50); // Lower-left
  } else if ((mouseX > 50) && (mouseY < 50)) {
    rect(50, 0, 50, 50); // Upper-right
  } else {
    rect(50, 50, 50, 50); // Lower-right
  }
}
