float y = 50.0;
float radius = 15.0;
float velocity = 8.0;
float friction = 0.98;

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
  velocity *= friction;
  y += velocity;
  if (y > height + radius) {
    y = -radius;
  }
}
