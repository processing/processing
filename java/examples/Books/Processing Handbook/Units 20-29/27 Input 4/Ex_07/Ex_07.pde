float x = 0;
float y = 0;
float easing = 0.05; // Numbers 0.0 to 1.0
void setup() {
  size(100, 100);
  smooth();
  noStroke();
}

void draw() {
  background(0);
  float targetX = mouseX;
  float targetY = mouseY;
  x += (targetX - x) * easing;
  y += (targetY - y) * easing;
  fill(153);
  ellipse(mouseX, mouseY, 20, 20);
  fill(255);
  ellipse(x, y, 40, 40);
}