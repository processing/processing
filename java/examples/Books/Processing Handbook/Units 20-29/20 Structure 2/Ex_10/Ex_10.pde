int d = 51; // Variable d can be used everywhere

void setup() {
  size(100, 100);
  int val = d * 2; // Local variable val can only be used in setup()
  fill(val);
}

void draw() {
  int y = 60; // Local variable y can only be used in draw()
  line(0, y, d, y);
  y -= 25;
  line(0, y, d, y);
}