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
  pushMatrix();
  translate(0, y);

  // Affected by first translate()
  ellipse(33, 0, radius, radius);
  translate(0, y);
  
  // Affected by first and second translate()
  ellipse(66, 0, radius, radius);
  popMatrix();
  
  // Not affected by either translate()
  ellipse(99, 50, radius, radius);
  y = y + speed;
  if (y > height + radius) {
    y = -radius;
  }
}
