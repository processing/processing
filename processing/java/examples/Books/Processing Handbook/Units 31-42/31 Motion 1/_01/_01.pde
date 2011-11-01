float y = 50.0;
float speed = 1.0;
float radius = 15.0;

void setup() {
  size(100, 100);
  smooth();
  noStroke();
  ellipseMode(RADIUS);
}

void draw() {
  background(0);
  ellipse(33, y, radius, radius);
  y = y + speed;
  if (y > height+radius) {
    y = -radius;
  }
}