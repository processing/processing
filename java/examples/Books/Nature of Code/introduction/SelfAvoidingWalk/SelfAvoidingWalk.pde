// Daniel Shiffman
// The Nature of Code
// http://www.shiffman.net/

Walker w;

void setup() {
  size(600,400);
  // Create a walker object
  w = new Walker();
  background(255);
}

void draw() {
  // Run the walker object
  w.step();
  w.render();
}


