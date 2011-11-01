int x = 50; // X-coordinate of the center
int y = 100; // Y-coordinate of the bottom
int a = 35; // Half the width of the top bar
int n = 3; // Number of recursions

void setup() {
  size(100, 100);
  noLoop();
}

void draw() {
  drawT(x, y, a, n);
}

void drawT(int x, int y, int apex, int num) {
  line(x, y, x, y-apex);
  line(x-apex, y-apex, x+apex, y-apex);
  // This relational expression must eventually be
  // false to stop the recursion and draw the lines
  if (num > 0) {
    drawT(x-apex, y-apex, apex/2, num-1);
    drawT(x+apex, y-apex, apex/2, num-1);
  }
}