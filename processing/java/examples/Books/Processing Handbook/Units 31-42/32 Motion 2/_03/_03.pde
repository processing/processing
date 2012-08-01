float angle = 0.0;
float speed = 0.1;

void setup() {
  size(100, 100);
  noStroke();
  smooth();
}

void draw() {
  background(0);
  angle = angle + speed;
  ellipse(50 + (sin(angle + PI) * 5), 25, 30, 30);
  ellipse(50 + (sin(angle + HALF_PI) * 5), 55, 30, 30);
  ellipse(50 + (sin(angle + QUARTER_PI) * 5), 85, 30, 30);
}
