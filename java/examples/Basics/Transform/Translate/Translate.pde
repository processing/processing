/**
 * Translate. 
 * 
 * The translate() function allows objects to be moved
 * to any location within the window. The first parameter
 * sets the x-axis offset and the second parameter sets the
 * y-axis offset. 
 */
 
float x, y;
float dim = 80.0;

void setup() {
  size(640, 360);
  noStroke();
}

void draw() {
  background(102);
  
  x = x + 0.8;
  
  if (x > width + dim) {
    x = -dim;
  } 
  
  translate(x, height/2-dim/2);
  fill(255);
  rect(-dim/2, -dim/2, dim, dim);
  
  // Transforms accumulate. Notice how this rect moves 
  // twice as fast as the other, but it has the same 
  // parameter for the x-axis value
  translate(x, dim);
  fill(0);
  rect(-dim/2, -dim/2, dim, dim);
}
