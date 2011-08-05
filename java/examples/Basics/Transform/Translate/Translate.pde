/**
 * Translate. 
 * 
 * The translate() function allows objects to be moved
 * to any location within the window. The first parameter
 * sets the x-axis offset and the second parameter sets the
 * y-axis offset. 
 */
 
float x, y;
float size = 80.0;

void setup() {
  size(640, 360);
  noStroke();
}

void draw() {
  background(102);
  
  x = x + 0.8;
  
  if (x > width + size) {
    x = -size;
  } 
  
  translate(x, height/2-size/2);
  fill(255);
  rect(-size/2, -size/2, size, size);
  
  // Transforms accumulate.
  // Notice how this rect moves twice
  // as fast as the other, but it has
  // the same parameter for the x-axis value
  translate(x, size);
  fill(0);
  rect(-size/2, -size/2, size, size);
}
