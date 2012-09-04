// Simple Recursion
// Daniel Shiffman <http://www.shiffman.net>
// Nature of Code, Chapter 8

void setup() {
  size(800, 200);  
  smooth();
}

void draw() {
  background(255);
  drawCircle(width/2, height/2, 400);
  noLoop();
}

void drawCircle(float x, float y, float radius) {
  noFill();
  stroke(0);
  ellipse(x, y, radius, radius);
  if (radius > 8) {
    // Four circles! left right, up and down
    drawCircle(x + radius/2, y, radius/2);
    drawCircle(x - radius/2, y, radius/2);
    drawCircle(x, y + radius/2, radius/2);
    drawCircle(x, y - radius/2, radius/2);
  }
}

