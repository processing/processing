// Example 05-03 from "Getting Started with Processing" 
// by Reas & Fry. O'Reilly / Make 2010

int x = 280;
int y = -100;
int diameter = 380;

void setup() {
  size(480, 120);
  smooth();
  fill(102);
}

void draw() {
  background(204);
  ellipse(x, y, diameter, diameter);
}

