float fuzzy = 0.0;

void setup() {
  size(100, 100);
  smooth();
  strokeWeight(5);
  noFill();
}

void draw() {
  background(204);
  if (fuzzy < 16.0) {
    fuzzy += 0.05;
  }
  line(0, 30, 100, 60);
  filter(BLUR, fuzzy);
  line(0, 50, 100, 80);
}
