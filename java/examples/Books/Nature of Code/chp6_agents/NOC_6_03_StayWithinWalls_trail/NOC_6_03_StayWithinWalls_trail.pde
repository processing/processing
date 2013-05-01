// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Stay Within Walls
// "Made-up" Steering behavior to stay within walls


Vehicle v;
boolean debug = true;

float d = 25;


void setup() {
  size(640, 360);
  v = new Vehicle(width/2, height/2);
}

void draw() {
  background(255);

  if (debug) {
    stroke(175);
    noFill();
    rectMode(CENTER);
    rect(width/2, height/2, width-d*2, height-d*2);
  }

  v.boundaries();
  v.run();
}

void mousePressed() {
  debug = !debug;
}

