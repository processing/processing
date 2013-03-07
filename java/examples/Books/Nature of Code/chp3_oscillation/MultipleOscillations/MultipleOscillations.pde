// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

float angle1 = 0;
float aVelocity1 = 0.01;
float amplitude1 = 300;
  
float angle2 = 0;
float aVelocity2 = 0.3;
float amplitude2 = 10;


void setup() {
  size(640,360);
}

void draw() {
  background(255);
  
  float x = 0;
  x += amplitude1 * cos(angle1);
  x += amplitude2 * sin(angle2);

  angle1 += aVelocity1;
  angle2 += aVelocity2;
  
  ellipseMode(CENTER);
  stroke(0);
  fill(175);
  translate(width/2,height/2);
  line(0,0,x,0);
  ellipse(x,0,20,20);
}
