FixedSpring s;
float gravity = 0.5;

void setup() {
  size(100, 100);
  smooth();
  fill(0);
// Inputs: x, y, mass, gravity, length
  s = new FixedSpring(0.0, 50.0, 1.0, gravity, 40.0);
}

void draw() {
  background(204);
  s.update(mouseX, mouseY);
  s.display(mouseX, mouseY);
}
