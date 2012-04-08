float x = 0.0;
float easing = 0.05; // Numbers 0.0 to 1.0

void setup() {
  size(100, 100);
  smooth();
}

void draw() {
  background(0);
  float targetX = mouseX;
  x += (targetX - x) * easing;
  ellipse(mouseX, 30, 40, 40);
  ellipse(x, 70, 40, 40);
}