Scrollbar bar1, bar2;
PFont font;

void setup() {
  size(100, 100);
  noStroke();
// Inputs: x, y, width, height, minVal, maxVal
  bar1 = new Scrollbar(10, 35, 80, 10, 0.0, 100.0);
  bar2 = new Scrollbar(10, 55, 80, 10, 0.0, 1.0);
  font = loadFont("Courier-30.vlw");
  textFont(font);
  textAlign(CENTER);
}

void draw() {
  background(204);
  fill(0);
  int pos1 = int(bar1.getPos());
  text(nf(pos1, 2), 50, 30);
  float pos2 = bar2.getPos();
  text(nf(pos2, 1, 2), 50, 90);
  bar1.update(mouseX, mouseY);
  bar2.update(mouseX, mouseY);
  bar1.display();
  bar2.display();
}

void mousePressed() {
  bar1.press(mouseX, mouseY);
  bar2.press(mouseX, mouseY);
}

void mouseReleased() {
  bar1.release();
  bar2.release();
}
