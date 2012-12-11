// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A Polar coordinate, radius now starts at 0 to spiral outwards
float r = 0;
float theta = 0;

void setup() {
  size(750,200);
  background(255);
  smooth();
}

void draw() {
  // Polar to Cartesian conversion
  float x = r * cos(theta);
  float y = r * sin(theta);

  // Draw an ellipse at x,y
  noStroke();
  fill(0);
  // Adjust for center of window
  ellipse(x+width/2, y+height/2, 16, 16); 

  // Increment the angle
  theta += 0.01;
  // Increment the radius
  r += 0.05;
}
