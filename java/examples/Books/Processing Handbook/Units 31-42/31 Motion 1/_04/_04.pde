float d = 20.0;
float speed = 1.0;
int direction = 1;

void setup() {
  size(100, 100);
  smooth();
  noStroke();
  fill(255, 204);
}

void draw() {
  background(0);
  ellipse(0, 50, d, d);
  ellipse(100, 50, d, d);
  ellipse(50, 0, d, d);
  ellipse(50, 100, d, d);
  d += speed * direction;
  if ((d > width) || (d < width / 10)) {
    direction = -direction;
  }
}



