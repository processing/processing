// Example 05-05 from "Getting Started with Processing" 
// by Reas & Fry. O'Reilly / Make 2010

void setup() {
  size(480, 120);
  fill(0, 102);
  smooth();
  noStroke();
}

void draw() {
  background(204);
  ellipse(mouseX, mouseY, 9, 9);
}

