// Noise Walker
// Daniel Shiffman <http://www.shiffman.net>
// The Nature of Code

Walker w;

void setup() {
  size(800, 200);
  smooth();
  frameRate(30);

  // Create a walker object
  w = new Walker();
}

void draw() {
  background(255);
  // Run the walker object
  w.walk();
  w.display();
}


