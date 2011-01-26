void setup() {
  size(100, 100);
  noLoop();
  smooth();
}

void draw() {
  drawX(160, 20, 0, 5, 60); // Draw thick, light gray X
  drawX(0, 10, 30, 20, 60); // Draw medium, black X
  drawX(255, 2, 20, 38, 60); // Draw thin, white X
}

void drawX(int gray, int weight, int x, int y, int size) {
  stroke(gray);
  strokeWeight(weight);
  line(x, y, x+size, y+size);
  line(x+size, y, x, y+size);
}
