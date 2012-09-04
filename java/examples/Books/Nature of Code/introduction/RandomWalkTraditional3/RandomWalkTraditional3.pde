// Daniel Shiffman
// The Nature of Code
// http://www.shiffman.net/

Walker w;

void setup() {
  size(200,200);
  // Create a walker object
  w = new Walker();
  background(0);
}

void draw() {
  // Run the walker object
  w.step();
  w.render();
}


