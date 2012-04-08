FixedSpring s1, s2;
float gravity = 1.2;

void setup() {
  size(100, 100);
  smooth();
  fill(0);
// Inputs: x, y, mass, gravity, length
  s1 = new FixedSpring(45, 33, 1.5, gravity, 40.0);
  s2 = new FixedSpring(55, 66, 1.5, gravity, 40.0);
}

void draw() {
  background(204);
  s1.update(s2.x, s2.y);
  s2.update(s1.x, s1.y);
  s1.display(s2.x, s2.y);
  s2.display(s1.x, s1.y);
  if (mousePressed == true) {
    s1.x = mouseX;
    s1.y = mouseY;
  }
}
