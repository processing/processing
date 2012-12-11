// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

Mover mover;

void setup() {
  size(800,200);
  mover = new Mover(); 
}

void draw() {
  background(255);
  
  mover.update();
  mover.checkEdges();
  mover.display(); 
}

