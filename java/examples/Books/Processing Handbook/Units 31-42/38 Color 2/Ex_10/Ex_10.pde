color olive, gray;

void setup() {
  size(100, 100);
  colorMode(HSB, 360, 100, 100, 100);
  noStroke();
  smooth();
  olive = color(75, 61, 59);
  gray = color(30, 17, 42);
}

void draw() {
  float y = mouseY / float(height);
  background(gray);
  fill(olive);
  quad(70 + y*6, 0, 100, 0, 100, 100, 30 - y*6, 100);
  color yellow = color(48 + y * 20, 100, 88 - y * 20);
  fill(yellow);
  ellipse(50, 45 + y*10, 60, 60);
  color orange = color(29, 100, 83 - y * 10);
  fill(orange);
  ellipse(54, 42 + y*16, 24, 24);
}
