int x = 0;
void setup() {
  size(100, 100);
}

void draw() {
  float sec = millis() / 1000.0;
  if (sec > 3.0) {
    x++;
  }
  line(x, 0, x, 100);
}