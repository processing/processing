/**
 * Inspired by Ira Greenberg's RadialGradient sketch,
 * but uses a different method for the gradients.
 */

int dim = 40;

void setup() {
  size(200, 200);
  background(0);
  smooth();
  noStroke();
  ellipseMode(RADIUS);

  // create a simple table of gradients
  int rows = height / dim;
  int cols = width / dim;

  for (int row = 0; row < rows; row++) {
    for (int col = 0; col < cols; col++) {
      drawGradient(col*dim + dim/2, row*dim + dim/2);
    }
  }
}

void drawGradient(float x, float y) {
  int radius = dim/2 - 2;
  float r1 = random(255);
  float g1 = random(255);
  float b1 = random(255);
  float dr = (random(255) - r1) / radius;
  float dg = (random(255) - g1) / radius;
  float db = (random(255) - b1) / radius;
  
  for (int r = radius; r > 0; --r) {
    fill(r1, g1, b1);
    ellipse(x, y, r, r);
    r1 += dr;
    g1 += dg;
    b1 += db;
  }
}

