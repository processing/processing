/**
 * SVGPShape
 * 
 * How to load an SVG into a PShape 
 */

// PShape object
PShape svg;

void setup() {
  size(640, 360, P2D);
  smooth();
  // Load the SVG
  svg = loadShape("star.svg");
}

void draw() {
  background(255);
  // Draw PShape at mouse location
  translate(mouseX, mouseY);
  shapeMode(CENTER);
  shape(svg);
}

