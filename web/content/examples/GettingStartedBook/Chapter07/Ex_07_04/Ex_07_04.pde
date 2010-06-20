int radius = 40;
float x = -radius;
float speed = 0.5;

void setup() {
  size(240, 120);
  smooth();
  ellipseMode(RADIUS);
}

void draw() {
  background(0);
  x += speed;  // Increase the value of x
  if (x > width+radius) {  // If the shape is off screen
    x = -radius;  // move to the left edge
  }
  arc(x, 60, radius, radius, 0.52, 5.76);
}

