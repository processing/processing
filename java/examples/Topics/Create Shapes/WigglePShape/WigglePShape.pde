/**
 * WigglePShape. 
 * 
 * How to move the individual vertices of a PShape
 */


// A "Wiggler" object
Wiggler w;

void setup() {
  size(640, 360, P3D);
  smooth();
  w = new Wiggler();
}

void draw() {
  background(255);
  w.display();
  w.wiggle();
}


