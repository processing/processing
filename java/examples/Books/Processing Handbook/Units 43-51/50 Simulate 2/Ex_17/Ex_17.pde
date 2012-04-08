int numSprings = 30;
Spring2D[] s = new Spring2D[numSprings];
float gravity = 5.0;
float mass = 3.0;

void setup() {
  size(100, 900);
  smooth();
  fill(0);
  for (int i = 0; i < numSprings; i++) {
    s[i] = new Spring2D(width / 2, i*(height / numSprings), mass, gravity);
  }
}

void draw() {
  background(204);
  s[0].update(mouseX, mouseY);
  s[0].display(mouseX, mouseY);
  for (int i = 1; i < numSprings; i++) {
    s[i].update(s[i-1].x, s[i-1].y);
    s[i].display(s[i-1].x, s[i-1].y);
  }
}
