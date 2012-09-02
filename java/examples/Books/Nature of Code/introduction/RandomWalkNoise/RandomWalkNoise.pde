// Daniel Shiffman
// The Nature of Code
// http://www.shiffman.net/

Walker w;

void setup() {
  size(640,360);
  smooth();
  w = new Walker();
  background(0);
}

void draw() {
  // Run the walker object
  w.step();
  w.render();
}


