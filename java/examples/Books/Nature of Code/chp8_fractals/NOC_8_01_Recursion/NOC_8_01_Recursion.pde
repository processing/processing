// Simple Recursion
// Daniel Shiffman <http://www.shiffman.net>
// Nature of Code, Chapter 8

void setup() {
  size(800,200);  
  smooth();
}

void draw() {
  background(255);
  drawCircle(width/2,height/2,width); 
  noLoop();
}

// Very simple function that draws one circle 
// and recursively calls itself
void drawCircle(int x, int y, float r) {
  ellipse(x, y, r, r);
  // Exit condition, stop when radius is too small
  if(r > 2) {
    r *= 0.75f;
    // Call the function inside the function! (recursion!)
    drawCircle(x, y, r);	 
  }				
}
