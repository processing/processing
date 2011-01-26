// Example 06-10 from "Getting Started with Processing" 
// by Reas & Fry. O'Reilly / Make 2010

PShape network;

void setup() {
  size(240, 120);
  smooth();
  shapeMode(CENTER);
  network = loadShape("network.svg");
}

void draw() {
  background(0);
  float diameter = map(mouseX, 0, width, 10, 800);
  shape(network, 120, 60, diameter, diameter);
}

