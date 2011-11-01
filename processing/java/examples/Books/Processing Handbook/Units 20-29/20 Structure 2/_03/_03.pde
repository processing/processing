float y = 0.0;
void draw() {
  frameRate(30);
  line(0, y, 100, y);
  y = y + 0.5;
}