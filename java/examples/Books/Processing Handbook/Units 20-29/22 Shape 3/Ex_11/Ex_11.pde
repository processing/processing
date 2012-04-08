int x = 63; // X-coordinate
int y = 50; // Y-coordinate
int r = 80; // Starting radius
int n = 7; // Number of recursions
int rs = 12; // Random seed value

void setup() {
  size(100, 100);
  noStroke();
  smooth();
  noLoop();
  randomSeed(rs);
}

void draw() {
  drawCircle(x, y, r, n);
}

void drawCircle(float x, float y, int radius, int num) {
  float value = 126 * num / 6.0;
  fill(value, 153);
  ellipse(x, y, radius*2, radius*2);
  if (num > 1) {
    num = num - 1;
    int branches = int(random(2, 6));
    for (int i = 0; i < branches; i++) {
      float a = random(0, TWO_PI);
      float newx = x + cos(a) * 6.0 * num;
      float newy = y + sin(a) * 6.0 * num;
      drawCircle(newx, newy, radius/2, num);
    }
  }
}