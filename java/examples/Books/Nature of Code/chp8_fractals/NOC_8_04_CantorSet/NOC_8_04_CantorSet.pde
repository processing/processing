// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Cantor Set
// Renders a simple fractal, the Cantor Set

void setup() {
  size(800, 200);
  background(255);
  
  // Call the recursive function
  cantor(35, 0, 730);
}

void draw() {
  // No need to loop
  noLoop();
}


void cantor(float x, float y, float len) {
  
  float h = 30;
  
  // recursive exit condition
  if (len >= 1) {
    // Draw line (as rectangle to make it easier to see)
    noStroke();
    fill(0);
    rect(x, y, len, h/3);
    // Go down to next y position
    y += h;
    // Draw 2 more lines 1/3rd the length (without the middle section)
    cantor(x, y, len/3);
    cantor(x+len*2/3, y, len/3);
  }
}

