int numButtons = 7;
Radio[] buttons = new Radio[numButtons];

void setup() {
  size(100, 100);
  smooth();
  for (int i = 0; i < buttons.length; i++) {
    int x = i * 12 + 14;
    buttons[i] = new Radio(x, 50, 10, color(255), color(0),
                           i, buttons);
  }
}

void draw() {
  background(204);
  for (int i = 0; i < buttons.length; i++) {
    buttons[i].display();
  }
}

void mousePressed() {
  for (int i = 0; i < buttons.length; i++) {
    buttons[i].press(mouseX, mouseY);
  }
}
