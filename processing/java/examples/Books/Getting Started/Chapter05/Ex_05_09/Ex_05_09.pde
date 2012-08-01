// Example 05-09 from "Getting Started with Processing" 
// by Reas & Fry. O'Reilly / Make 2010

float x;
float y;
float px;
float py;
float easing = 0.05;

void setup() {
  size(480, 120);
  smooth();
  stroke(0, 102);
}

void draw() {
  float targetX = mouseX;
  x += (targetX - x) * easing;
  float targetY = mouseY;
  y += (targetY - y) * easing;
  float weight = dist(x, y, px, py);
  strokeWeight(weight);
  line(x, y, px, py);
  py = y;
  px = x;
}

