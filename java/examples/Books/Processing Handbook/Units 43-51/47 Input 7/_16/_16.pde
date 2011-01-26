// Requires Scrollbar Class
Scrollbar bar;
PImage img;

void setup() {
  size(100, 100);
  noStroke();
// Inputs: x, y, width, height, minVal, maxVal
  bar = new Scrollbar(10, 45, 80, 10, -200.0, 0.0);
  img = loadImage("landscape.jpg");
}

void draw() {
  background(204);
  int x = int(bar.getPos());
  image(img, x, 0);
  bar.update(mouseX, mouseY);
  bar.display();
}

void mousePressed() {
  bar.press(mouseX, mouseY);
}

void mouseReleased() {
  bar.release();
}
