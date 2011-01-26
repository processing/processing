// Example 07-03 from "Getting Started with Processing" 
// by Reas & Fry. O'Reilly / Make 2010

int radius = 40;
float x = -radius;
float speed = 0.5;

void setup() {
  size(240, 120);
  smooth();
  ellipseMode(RADIUS);
}

void draw() {
  background(0);
  x += speed;  // Increase the value of x
  arc(x, 60, radius, radius, 0.52, 5.76);
}

