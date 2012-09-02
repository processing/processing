// Koch Curve
// Daniel Shiffman <http://www.shiffman.net>
// Nature of Code, Chapter 8

// Renders a simple fractal, the Koch snowflake
// Each recursive level drawn in sequence

KochFractal k;

void setup() {
  size(800,250);
  background(255);
  frameRate(1);  // Animate slowly
  k = new KochFractal();
  smooth();
}

void draw() {
  background(255);
  // Draws the snowflake!
  k.render();
  // Iterate
  k.nextLevel();
  // Let's not do it more than 5 times. . .
  if (k.getCount() > 5) {
    k.restart();
  }
}

