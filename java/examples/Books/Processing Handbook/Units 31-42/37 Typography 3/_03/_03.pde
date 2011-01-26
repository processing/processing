// The horizontal position of the mouse determines the
// rotation angle. The angle accumulates with each letter
// drawn to make the typography curve.
String word = "Flexibility";
PFont f;
char[] letters;

void setup() {
  size(100, 100);
  f = loadFont("Eureka-24.vlw");
  textFont(f);
  letters = word.toCharArray();
  fill(0);
}

void draw() {
  background(204);
  pushMatrix();
  translate(0, 33);
  for (int i = 0; i < letters.length; i++) {
    float angle = map(mouseX, 0, width, 0, PI / 8);
    rotate(angle);
    text(letters[i], 0, 0);
// Offset by the width of the current letter
    translate(textWidth(letters[i]), 0);
  }
  popMatrix();
}
