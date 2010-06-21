// Example 05-11 from "Getting Started with Processing" 
// by Reas & Fry. O'Reilly / Make 2010

void setup() {
  size(240, 120);
  strokeWeight(12);
  smooth();
}

void draw() {
  background(204);
  stroke(255);
  line(120, 60, mouseX, mouseY); // White line
  stroke(0);
  float mx = map(mouseX, 0, width, 60, 180);
  line(120, 60, mx, mouseY); // Black line
}

