// Daniel Shiffman
// The Nature of Code
// http://natureofcode.com

Walker w;

void setup() {
  size(640,480);
  // Create a walker object
  w = new Walker();
  background(0);
}

void draw() {
  // Run the walker object
  w.step();
  w.render();
}


