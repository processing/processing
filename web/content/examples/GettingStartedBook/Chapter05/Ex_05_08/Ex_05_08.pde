float x;
float easing = 0.01;
float diameter = 12;

void setup() {
  size(220, 120);
  smooth();
}

void draw() {
  float targetX = mouseX;
  x += (targetX - x) * easing;
  ellipse(x, 40, 12, 12);
  println(targetX + " : " + x);
}

