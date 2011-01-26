// Example 05-13 from "Getting Started with Processing" 
// by Reas & Fry. O'Reilly / Make 2010

void setup() {
  size(240, 120);
  smooth();
  strokeWeight(30);
}

void draw() {
  background(204);
  stroke(102);
  line(40, 0, 70, height);
  if (mousePressed) {
    stroke(0);
  } else {
    stroke(255);
  }
  line(0, 70, width, 50);
}

