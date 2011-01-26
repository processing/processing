// Example 06-09 from "Getting Started with Processing" 
// by Reas & Fry. O'Reilly / Make 2010

PShape network;

void setup() {
  size(480, 120);
  smooth();
  network = loadShape("network.svg");
}

void draw() {
  background(0);
  shape(network, 30, 10);
  shape(network, 180, 10, 280, 280);
}

