// Set the square to black when the left mouse button
// is pressed and white when the right button is pressed
void setup() {
  size(100, 100);
}

void draw() {
  if (mouseButton == LEFT) {
    fill(0); // Black
  } else if (mouseButton == RIGHT) {
    fill(255); // White
  } else {
    fill(126); // Gray
  }
  rect(25, 25, 50, 50);
}