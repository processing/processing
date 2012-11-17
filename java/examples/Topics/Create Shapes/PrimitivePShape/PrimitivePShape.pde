/**
 * PrimitivePShape. 
 * 
 * Using a PShape to display a primitive shape (in this case, ellipse). 
 */


// The PShape object
PShape circle;

void setup() {  
  size(640, 360, P2D);
  smooth();
  // Creating the PShape as an ellipse
  // The corner is -50,-50 so that the center is at 0,0 
  circle = createShape(ELLIPSE, -50, -25, 100, 50);
}

void draw() {
  background(51);
  // We can dynamically set the stroke and fill of the shape
  circle.stroke(255);  
  circle.strokeWeight(4);
  circle.fill(map(mouseX, 0, width, 0, 255));
  // We can use translate to move the PShape
  translate(mouseX, mouseY);
  // Drawing the PShape
  shape(circle);
}

