float angle = 0.0;

void setup() {
  size(100, 100);
  smooth();
  noStroke();
}

void draw() {
  fill(0, 12);
  rect(0, 0, width, height);
  fill(255);
  angle = angle + 0.02;
  translate(70, 40);
  rotate(angle);
  rect(-30, -30, 60, 60);
}
