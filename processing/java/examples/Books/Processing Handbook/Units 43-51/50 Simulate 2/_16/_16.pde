Spring2D s1, s2;
float gravity = 5.0;
float mass = 2.0;

void setup() {
  size(100, 100);
  smooth();
  fill(0);
// Inputs: x, y, mass, gravity
  s1 = new Spring2D(0.0, width / 2, mass, gravity);
  s2 = new Spring2D(0.0, width / 2, mass, gravity);
}

void draw() {
  background(204);
  s1.update(mouseX, mouseY);
  s1.display(mouseX, mouseY);
  s2.update(s1.x, s1.y);
  s2.display(s1.x, s1.y);
}
