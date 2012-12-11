// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Flow Field Following

// Via Reynolds: http://www.red3d.com/cwr/steer/FlowFollow.html

// Flowfield object
FlowField flowfield;
PShape arrow;
PImage a;

void setup() {
  size(1800, 60*9);
  // Make a new flow field with "resolution" of 16
  flowfield = new FlowField(60);
  arrow = loadShape("arrow.svg");
  a = loadImage("arrow60.png");
}

void draw() {
  background(255);
  // Display the flowfield in "debug" mode
  translate(30,30);
  flowfield.display();
  saveFrame("ch6_exc6.png");
  noLoop();
}
// Make a new flowfield
void mousePressed() {
  flowfield.init();
}


