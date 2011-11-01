// Requires the OverRect and OverCircle classes
OverRect r = new OverRect(9, 30, 36);
OverCircle c = new OverCircle(72, 48, 40);

void setup() {
  size(100, 100);
  noStroke();
  smooth();
}

void draw() {
  background(204);
  r.update(mouseX, mouseY);
  r.display();
  c.update(mouseX, mouseY);
  c.display();
}
