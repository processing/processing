DragButton icon;

void setup() {
  size(100, 100);
  smooth();
  color gray = color(204);
  color white = color(255);
  color black = color(0);
  icon = new DragButton(21, 42, 50, gray, white, black);
}

void draw() {
  background(204);
  icon.update(mouseX, mouseY);
  icon.display();
}

void mousePressed() {
  icon.press(mouseX, mouseY);
}

void mouseReleased() {
  icon.release();
}

void mouseDragged() {
  icon.drag(mouseX, mouseY);
}
