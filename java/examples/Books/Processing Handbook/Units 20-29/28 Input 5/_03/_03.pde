PFont font;

void setup() {
  size(100, 100);
  font = loadFont("Pro-20.vlw");
  textFont(font);
}

void draw() {
  background(0);
  int s = second();
  int m = minute();
  int h = hour();
  // The nf() function spaces the numbers nicely
  String t = nf(h,2) + ":" + nf(m,2) + ":" + nf(s,2);
  text(t, 10, 55);
}