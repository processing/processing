// Gradually increases the velocity
float y = 50.0;
float radius = 15.0;
float velocity = 0.0;
float acceleration = 0.01;

void setup() {
  size(100, 100);
  smooth();
  noStroke();
  ellipseMode(RADIUS);
}

void draw() {
  fill(0, 10);
  rect(0, 0, width, height);
  fill(255);
  ellipse(33, y, radius, radius);
  velocity += acceleration; // Increase the velocity
  y += velocity; // Update the position
  if (y > height + radius) { // If over the bottom edge,
    y = -radius; // move to the top
  }
}
