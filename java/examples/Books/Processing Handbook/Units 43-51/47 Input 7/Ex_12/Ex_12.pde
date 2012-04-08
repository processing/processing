Radio[] buttons = new Radio[2];

void setup() {
  size(100, 100);
  smooth();
// Inputs: x, y, size, base color, fill color,
// id number, array of others
  buttons[0] = new Radio(33, 50, 30, color(255), color(0),
                         0, buttons);
  buttons[1] = new Radio(66, 50, 30, color(255), color(0),
                         1, buttons);
}

void draw() {
  background(204);
  buttons[0].display();
  buttons[1].display();
}

void mousePressed() {
  buttons[0].press(mouseX, mouseY);
  buttons[1].press(mouseX, mouseY);
}
