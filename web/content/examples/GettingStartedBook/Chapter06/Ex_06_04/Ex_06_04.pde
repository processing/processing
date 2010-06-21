// Example 06-04 from "Getting Started with Processing" 
// by Reas & Fry. O'Reilly / Make 2010

PImage img;

void setup() {
  size(480, 120);
  img = loadImage("clouds.gif");
}

void draw() {
  background(255);
  image(img, 0, 0);
  image(img, 0, mouseY * -1);
}

