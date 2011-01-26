int x = 63; // X-coordinate
int r = 85; // Starting radius
int n = 6; // Number of recursions

void setup() {
  size(100, 100);
  noStroke();
  smooth();
  noLoop();
}

void draw() {
  drawCircle(63, 85, 6);
}

void drawCircle(int x, int radius, int num) {
  float tt = 126 * num/4.0;
  fill(tt);
  ellipse(x, 50, radius*2, radius*2);
  if (num > 1) {
    num = num - 1;
    drawCircle(x - radius/2, radius/2, num);
    drawCircle(x + radius/2, radius/2, num);
  }
}