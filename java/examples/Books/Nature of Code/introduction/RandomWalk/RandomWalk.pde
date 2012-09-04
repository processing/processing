// Random Walker (No Vectors)
// Daniel Shiffman <http://www.shiffman.net>
// The Nature of Code

Walker w;

void setup() {
  size(400,400);
  frameRate(30);

  // Create a walker object
  w = new Walker();

}

void draw() {
  background(255);
  // Run the walker object
  w.walk();
  w.render();
}



