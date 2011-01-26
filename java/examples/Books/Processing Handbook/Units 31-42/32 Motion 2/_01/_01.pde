float angle = 0.0; // Current angle
float speed = 0.1; // Speed of motion
float radius = 40.0; // Range of motion

void setup() {
  size(100, 100);
  noStroke();
  smooth();
}

void draw() {
  fill(0, 12);
  rect(0, 0, width, height);
  fill(255);
  angle += speed;
  float sinval = sin(angle);
  float yoffset = sinval * radius;
  ellipse(50, 50 + yoffset, 80, 80);
}
