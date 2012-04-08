float x1, y1, x2, y2;

void setup() {
  size(100, 100);
  smooth();
  x1 = width / 4.0;
  y1 = x1;
  x2 = width - x1;
  y2 = x2;
}

void draw() {
  background(204);
  x1 += random(-0.5, 0.5);
  y1 += random(-0.5, 0.5);
  x2 += random(-0.5, 0.5);
  y2 += random(-0.5, 0.5);
  line(x1, y1, x2, y2);
}
