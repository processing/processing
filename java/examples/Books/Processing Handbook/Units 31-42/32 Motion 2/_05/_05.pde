float x = 50.0; // X-coordinate
float y = 80.0; // Y-coordinate

void setup() {
  size(100, 100);
  randomSeed(0); // Force the same random values
  background(0);
  stroke(255);
}

void draw() {
  x += random(-2, 2); // Assign new x-coordinate
  y += random(-2, 2); // Assign new y-coordinate
  point(x, y);
}
