void setup() {
  size(100, 100);
}

void draw() {
  for (int i = -14; i <= 14; i += 2) {
    point(mouseX+i, mouseY);
  }
}