// The size of each letter grows and shrinks from
// left to right
PFont font;
String s = "AREA";
float angle = 0.0;

void setup() {
  size(100, 100);
  font = loadFont("EurekaMono-48.vlw");
  textFont(font);
  fill(0);
}

void draw() {
  background(204);
  angle += 0.1;
  for (int i = 0; i < s.length(); i++) {
    float c = sin(angle + i / PI);
    textSize((c + 1.0) * 32 + 10);
    text(s.charAt(i), i*26, 60);
  }
}
