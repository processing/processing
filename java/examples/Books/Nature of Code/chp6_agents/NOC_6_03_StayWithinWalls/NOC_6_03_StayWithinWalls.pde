// Stay Within Walls
// Daniel Shiffman <http://www.shiffman.net>
// The Nature of Code

// "Made-up" Steering behavior to stay within walls


Vehicle v;
boolean debug = true;

float d = 25;


void setup() {
  size(800, 200);
  v = new Vehicle(width/2, height/2);
  smooth();
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

