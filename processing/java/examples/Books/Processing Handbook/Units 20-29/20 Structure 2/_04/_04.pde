float y = 0.0;

void draw() {
  frameRate(30);
  background(204);
  y = y + 0.5;
  line(0, y, 100, y);
}