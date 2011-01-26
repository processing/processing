void setup() {
  size(100, 100);
}

void draw() {
  for (int i = 0; i < 50; i += 2) {
    point(mouseX+i, mouseY+i);
  }
}