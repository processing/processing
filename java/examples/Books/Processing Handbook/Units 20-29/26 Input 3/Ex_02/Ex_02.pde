float gray = 0;

void setup() {
  size(100, 100);
}

void draw() {
  background(gray);
}

void mouseReleased() {
  gray += 20;
}
