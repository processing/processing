float gray = 0;

void setup() {
  size(100, 100);
}

void draw() {
  background(gray);
}

void mousePressed() {
  gray += 20;
}
