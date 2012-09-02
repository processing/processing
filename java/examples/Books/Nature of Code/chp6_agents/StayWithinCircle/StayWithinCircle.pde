// Stay Within Walls
// Daniel Shiffman <http://www.shiffman.net>
// The Nature of Code

// "Made-up" Steering behavior to stay within walls


Vehicle v;
boolean debug = true;


PVector circleLocation;
float circleRadius;



void setup() {
  size(640, 360);
  v = new Vehicle(width/2, height/4);
  
  circleLocation = new PVector(width/2,height/2);
  circleRadius = height/2-25;
  
  smooth();
}

void draw() {
  background(255);

  if (debug) {
    stroke(175);
    noFill();
    ellipse(circleLocation.x,circleLocation.y, circleRadius*2,circleRadius*2);
  }

  v.boundaries();
  v.run();
}

void mousePressed() {
  debug = !debug;
}

