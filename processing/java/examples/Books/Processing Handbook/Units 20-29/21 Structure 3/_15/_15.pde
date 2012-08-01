void setup() {
  size(100, 100);
  smooth();
  noLoop();
}

void draw() {
  drawX(255); // Run first drawX()
  drawX(5.5); // Run second drawX()
  drawX(0, 2, 44, 48, 36); // Run third drawX()
}

// Draw an X with the gray value set by the parameter
void drawX(int gray) {
  stroke(gray);
  strokeWeight(20);
  line(0, 5, 60, 65);
  line(60, 5, 0, 65);
}

// Draw a black X with the thickness set by the parameter
void drawX(float weight) {
  stroke(0);
  strokeWeight(weight);
  line(0, 5, 60, 65);
  line(60, 5, 0, 65);
}

// Draws an X with the gray value, thickness,
// position, and size set by the parameters
void drawX(int gray, int weight, int x, int y, int s) {
  stroke(gray);
  strokeWeight(weight);
  line(x, y, x+s, y+s);
  line(x+s, y, x, y+s);
}