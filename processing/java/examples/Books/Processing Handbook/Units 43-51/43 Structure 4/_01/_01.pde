int x = 33;
int y = 50;
int diameter = 30;

void setup() {
  size(100, 100);
  smooth();
  noStroke();
}

void draw() {
  background(0);
  ellipse(x, y, diameter, diameter);
}
