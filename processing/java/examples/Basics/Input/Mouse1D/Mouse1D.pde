/**
 * Mouse 1D. 
 * 
 * Move the mouse left and right to shift the balance. 
 * The "mouseX" variable is used to control both the 
 * size and color of the rectangles. 
 */

void setup() {
  size(640, 360);
  noStroke();
  colorMode(RGB, height, height, height);
  rectMode(CENTER);
}

void draw() {
  background(0.0);

  float r1 = map(mouseX, 0, width, 0, height);
  float r2 = height-r1;
  
  fill(r1);
  rect(width/2 + r1/2, height/2, r1, r1);
  
  fill(r2);
  rect(width/2 - r2/2, height/2, r2, r2);

}

