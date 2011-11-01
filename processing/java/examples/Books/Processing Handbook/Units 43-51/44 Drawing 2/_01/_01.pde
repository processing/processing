void setup() {
  size(100, 100);
  smooth();
}

void draw() {
  float s = dist(mouseX, mouseY, pmouseX, pmouseY) + 1;
  noStroke();
  fill(0, 102);
  ellipse(mouseX, mouseY, s, s);
  stroke(255);
  point(mouseX, mouseY);
}
