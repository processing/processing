float speed = 0.0;
float easing = 0.05; // Numbers 0.0 to 1.0

void setup() {
  size(100, 100);
  noStroke();
  smooth();
}

void draw() {
  background(0);
  float target = dist(mouseX, mouseY, pmouseX, pmouseY);
  speed += (target - speed) * easing;
  rect(0, 33, target, 17);
  rect(0, 50, speed, 17);
}