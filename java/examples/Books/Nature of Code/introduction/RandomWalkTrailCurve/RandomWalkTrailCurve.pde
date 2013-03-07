// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

Walker w;

void setup() {
  size(400, 300);
  // Create a walker object
  w = new Walker();
}

void draw() {
  background(255);

  // Run the walker object
  w.step();
  w.render();
}

