int lastSecond = 0;

void setup() {
  size(100, 100);
}

void draw() {
  int s = second();
  int m = minute();
  int h = hour();
  // Only prints once when the second changes
  if (s != lastSecond) {
    println(h + ":" + m + ":" + s);
    lastSecond = s;
  }
}