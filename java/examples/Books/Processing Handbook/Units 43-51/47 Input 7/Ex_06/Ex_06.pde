Button button;

void setup() {
  size(100, 100);
// Inputs: x, y, size,
// base color, over color, press color
  button = new Button(25, 25, 50,
                      color(204), color(255), color(0));
}

void draw() {
  background(204);
  stroke(255);
  button.update();
  button.display();
}

void mousePressed() {
  button.press();
}

void mouseReleased() {
  button.release();
}
