PFont font;
String s = "Pea";

void setup() {
  size(100, 100);
  font = loadFont("Eureka-48.vlw");
  textFont(font);
  fill(0);
}

void draw() {
  background(204);
  text(s, 22, 20);
}
