/**
 * PathPShape
 * 
 * A simple path using PShape
 */

// A PShape object
PShape path;

void setup() {
  size(640, 360, P2D);
  orientation(LANDSCAPE);
  // Create the shape
  path = createShape();
  path.beginShape();
  // Set fill and stroke
  path.noFill();
  path.stroke(255);
  path.strokeWeight(2);
  
  float x = 0;
  // Calculate the path as a sine wave
  for (float a = 0; a < TWO_PI; a+=0.1) {
    path.vertex(x,sin(a)*100);
    x+= 5;
  }
  // The path is complete
  path.endShape();  

}

void draw() {
  background(51);
  // Draw the path at the mouse location
  translate(mouseX, mouseY);
  shape(path);
}

