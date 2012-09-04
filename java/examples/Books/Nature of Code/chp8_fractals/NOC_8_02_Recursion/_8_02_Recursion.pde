// Simple Recursion
// Daniel Shiffman <http://www.shiffman.net>
// Nature of Code, Chapter 8

void setup() {
  size(800,200);  
  smooth();
}

void draw() {
  background(255);
  drawCircle(width/2,height/2,400); 
  noLoop();
}

// Recursive function
void drawCircle(float x, float y, float r) {
  stroke(0);
  noFill();
  ellipse(x, y, r, r);
  if(r > 2) {
    // Now we draw two more circles, one to the left
    // and one to the right
    drawCircle(x + r/2, y, r/2);
    drawCircle(x - r/2, y, r/2);
  }
}
