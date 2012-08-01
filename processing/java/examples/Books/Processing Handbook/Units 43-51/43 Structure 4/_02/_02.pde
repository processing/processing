Spot sp; // Declare the object

void setup() {
  size(100, 100);
  smooth();
  noStroke();
  sp = new Spot(); // Construct the object
  sp.x = 33; // Assign 33 to the x field
  sp.y = 50; // Assign 50 to the y field
  sp.diameter = 30; // Assign 30 to the diameter field
}

void draw() {
  background(0);
  ellipse(sp.x, sp.y, sp.diameter, sp.diameter);
}

class Spot {
  float x, y; // The x- and y-coordinate
  float diameter; // Diameter of the circle
}
