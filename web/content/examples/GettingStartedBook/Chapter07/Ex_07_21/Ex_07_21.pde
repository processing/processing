// Example 07-21 from "Getting Started with Processing" 
// by Reas & Fry. O'Reilly / Make 2010

float angle = 0.0;

void setup() {
  size(120, 120);
  smooth();
}

void draw() {
  translate(mouseX, mouseY);
  scale(sin(angle) + 2);
  rect(-15, -15, 30, 30);
  angle += 0.1;
}

