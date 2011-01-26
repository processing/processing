void setup() {
  size(100, 100);
  smooth();
  noLoop();
  translate(50, 0); // Has no effect
}

void draw() {
  background(0);
  ellipse(0, 50, 60, 60);
}
