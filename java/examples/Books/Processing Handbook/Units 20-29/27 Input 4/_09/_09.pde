void setup() {
  size(100, 100);
  noStroke();
  smooth();
}

void draw() {
  background(0);
  float speed = dist(mouseX, mouseY, pmouseX, pmouseY);
  float diameter = speed * 3.0;
  ellipse(50, 50, diameter, diameter);
}