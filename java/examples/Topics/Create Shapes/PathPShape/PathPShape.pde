/**
 * PathPShape
 * 
 * A simple path using PShape
 */

// A PShape object
PShape path;

void setup() {
  size(640, 360, P3D);
  smooth();
  // Create the shape
  path = createShape();
  // Set fill and stroke
  path.noFill();
  path.stroke(0);
  path.strokeWeight(2);
  
  float x = 0;
  // Calculate the path as a sine wave
  for (float a = 0; a < TWO_PI; a+=0.1) {
    path.vertex(x,sin(a)*100);
    x+= 5;
  }
  // The path is complete
  path.end();  

}

void draw() {
  background(255);
  // Draw the path at the mouse location
  translate(mouseX, mouseY);
  shape(path);
}

