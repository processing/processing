float y = 50.0;
float radius = 15.0;
float velocity = 9.0;
float acceleration = -0.05;

void setup() {
  size(100, 100);
  smooth();
  noStroke();
  ellipseMode(RADIUS);
}

void draw() {
  fill(0, 12);
  rect(0, 0, width, height);
  fill(255);
  ellipse(33, y, radius, radius);
  velocity += acceleration;
  y += velocity;
  if (y > height + radius) {
    y = -radius;
  }
}
