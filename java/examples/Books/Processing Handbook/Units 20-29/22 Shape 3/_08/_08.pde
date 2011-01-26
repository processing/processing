int x = 50; // X-coordinate of the center
int y = 100; // Y-coordinate of the bottom
int a = 35; // Half the width of the top bar

void setup() {
  size(100, 100);
  noLoop();
}

void draw() {
  drawT(x, y, a);
}

void drawT(int xpos, int ypos, int apex) {
  line(xpos, ypos, xpos, ypos-apex);
  line(xpos-(apex/2), ypos-apex, xpos+(apex/2), ypos-apex);
}