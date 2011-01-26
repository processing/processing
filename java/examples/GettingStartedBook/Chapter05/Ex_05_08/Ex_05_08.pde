// Example 05-08 from "Getting Started with Processing" 
// by Reas & Fry. O'Reilly / Make 2010

float x;
float easing = 0.01;

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

