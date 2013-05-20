// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Angle Between Two Vectors
// Using the dot product to compute the angle between two vectors

void setup() {
  size(640, 360);
}

void draw() {
  background(255);

  // A "vector" (really a point) to store the mouse location and screen center location
  PVector mouseLoc = new PVector(mouseX, mouseY);
  PVector centerLoc = new PVector(width/2, height/2);  

  // Aha, a vector to store the displacement between the mouse and center
  PVector v = PVector.sub(mouseLoc, centerLoc);
  v.normalize();
  v.mult(75);

  PVector xaxis = new PVector(75, 0);
  // Render the vector
  drawVector(v, centerLoc, 1.0);
  drawVector(xaxis, centerLoc, 1.0);


  float theta = PVector.angleBetween(v, xaxis);

  fill(0);
  text(int(degrees(theta)) + " degrees\n" + theta + " radians", 10, 160);
}

// Renders a vector object 'v' as an arrow and a location 'loc'
void drawVector(PVector v, PVector loc, float scayl) {
  pushMatrix();
  float arrowsize = 6;
  // Translate to location to render vector
  translate(loc.x, loc.y);
  stroke(0);
  strokeWeight(2);
  // Call vector heading function to get direction (pointing up is a heading of 0)
  rotate(v.heading2D());
  // Calculate length of vector & scale it to be bigger or smaller if necessary
  float len = v.mag()*scayl;
  // Draw three lines to make an arrow 
  line(0, 0, len, 0);
  line(len, 0, len-arrowsize, +arrowsize/2);
  line(len, 0, len-arrowsize, -arrowsize/2);
  popMatrix();
}

