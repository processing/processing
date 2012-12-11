// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

float angle = 0;
float aVelocity = 0.05;

void setup() {
  size(640,360);
  smooth();
}

void draw() {
  background(255);
  
  float x = width/2;
  float y = map(sin(angle),-1,1,50,250);
  angle += aVelocity;
  
  ellipseMode(CENTER);
  stroke(0);
  fill(175);
  line(x,0,x,y);
  ellipse(x,y,20,20);
}
