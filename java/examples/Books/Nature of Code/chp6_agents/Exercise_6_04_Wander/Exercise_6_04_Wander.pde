// Wander
// Daniel Shiffman <http://www.shiffman.net>
// The Nature of Code

// Demonstration of Craig Reynolds' "Wandering" behavior
// See: http://www.red3d.com/cwr/

// Click mouse to turn on and off rendering of the wander circle


Vehicle wanderer;
boolean debug = true;

void setup() {
  size(740,200);
  wanderer = new Vehicle(width/2,height/2);
  smooth();
}

void draw() {
  background(255);
  wanderer.wander();
  wanderer.run();
}

void mousePressed() {
  debug = !debug;
}


