PFont font;

void setup() {
  size(100, 100);
  font = loadFont("ThesisMonoLight-72.vlw");
  textFont(font);
}

void draw() {
  background(0);
  text(key, 28, 75);
}