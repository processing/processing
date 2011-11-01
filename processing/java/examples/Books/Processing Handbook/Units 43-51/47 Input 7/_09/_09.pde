Check check;

void setup() {
  size(100, 100);
// Inputs: x, y, size, fill color
  check = new Check(25, 25, 50, color(0));
}

void draw() {
  background(204);
  check.display();
}

void mousePressed() {
  check.press(mouseX, mouseY);
}
