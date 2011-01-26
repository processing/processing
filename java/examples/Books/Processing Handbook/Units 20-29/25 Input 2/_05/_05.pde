int x = 0;

void setup() {
  size(100, 100);
}

void draw() {
  if (keyPressed == true) {
    x = key - 32;
    rect(x, -1, 20, 101);
  }
}