// Noise Walker
// Daniel Shiffman <http://www.shiffman.net>
// The Nature of Code

Walker w;

void setup() {
  size(640,360);
  smooth();

  // Create a walker object
  w = new Walker();

}

void draw() {
  background(255);
  // Run the walker object
  w.walk();
  w.display();
}



