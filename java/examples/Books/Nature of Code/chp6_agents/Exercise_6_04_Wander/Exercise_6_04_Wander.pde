// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Demonstration of Craig Reynolds' "Wandering" behavior
// See: http://www.red3d.com/cwr/

// Click mouse to turn on and off rendering of the wander circle

Vehicle wanderer;
boolean debug = true;

void setup() {
  size(640,360);
  wanderer = new Vehicle(width/2,height/2);
}

void draw() {
  background(255);
  wanderer.wander();
  wanderer.run();
}

void mousePressed() {
  debug = !debug;
}


