// Uses millis() to start a line in motion three seconds
// after the program starts
int x = 0;

void setup() {
  size(100, 100);
}

void draw() {
  if (millis() > 3000) {
    x++;
  }
  line(x, 0, x, 100);
}