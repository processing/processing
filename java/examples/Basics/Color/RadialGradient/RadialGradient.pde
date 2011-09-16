/**
 * Radial Gradient. 
 * 
 * Draws are series of concentric circles to create a gradient 
 * from one color to another.
 */

int dim;

void setup() {
  size(640, 360);
  dim = width/2;
  background(0);
  colorMode(HSB, 360, 100, 100);
  noStroke();
  ellipseMode(RADIUS);
  frameRate(1);
}

void draw() {
  background(0);
  for (int x = 0; x <= width; x+=dim) {
    drawGradient(x, height/2);
  } 
}

void drawGradient(float x, float y) {
  int radius = dim/2;
  float h = random(0, 360);
  for (int r = radius; r > 0; --r) {
    fill(h, 90, 90);
    ellipse(x, y, r, r);
    h = (h + 1) % 360;
  }
}

