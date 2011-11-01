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
  fill(0, 12);
  rect(0, 0, width, height);
  fill(255);
  translate(0, y); // Set the y-coordinate of the circle
  ellipse(33, 0, radius, radius);
  y += speed;
  if (y > height + radius) {
    y = -radius;
  }
}
