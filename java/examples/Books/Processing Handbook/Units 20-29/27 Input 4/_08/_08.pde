float x = 0.0;
float easing = 0.05; // Numbers 0.0 to 1.0

void setup() {
  size(100, 100);
  smooth();
}

void draw() {
  background(0);
  float targetX = mouseX;
  // Distance from position and target
  float dx = targetX - x;
  // If the distance between the current position and the
  // destination is greater than 1.0, update the position
  if (abs(dx) > 1.0) {
    x += dx * easing;
  }
  ellipse(mouseX, 30, 40, 40);
  ellipse(x, 70, 40, 40);
}