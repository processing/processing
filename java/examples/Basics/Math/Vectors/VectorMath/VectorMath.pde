/**
 * Vector 
 * by Daniel Shiffman.  
 * 
 * Demonstration some basic vector math: subtraction, normalization, scaling
 * Normalizing a vector sets its length to 1.
 */

void setup() {
  size(640,360);
  smooth();
}

void draw() {
  background(0);
  
  // A vector that points to the mouse location
  PVector mouse = new PVector(mouseX,mouseY);
  // A vector that points to the center of the window
  PVector center = new PVector(width/2,height/2);
  // Subtract center from mouse which results in a vector that points from center to mouse
  mouse.sub(center);
  
  // Normalize the vector
  mouse.normalize();
  
  // Multiply its length by 150 (Scaling its length)
  mouse.mult(150);

  translate(width/2,height/2);
  // Draw the resulting vector
  stroke(255);
  line(0,0,mouse.x,mouse.y);
  
}


