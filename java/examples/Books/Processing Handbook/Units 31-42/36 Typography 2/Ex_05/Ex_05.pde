PFont font;
String s = "VERTIGO";
float angle = 0.0;

void setup() {
  size(100, 100);
  font = loadFont("Eureka-90.vlw");
  textFont(font, 24);
  fill(0);
}

void draw() {
  background(204);
  angle += 0.02;
  pushMatrix();
  translate(33, 50);
  scale((cos(angle / 4.0) + 1.2) * 2.0);
  rotate(angle);
  text(s, 0, 0);
  popMatrix();
}
