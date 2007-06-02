/**
 * Follow 1. 
 * Based on code from Keith Peters (www.bit-101.com). 
 * 
 * A line segment is pushed and pulled by the cursor.
 */

float x = 100;
float y = 100;
float angle1 = 0.0;
float segLength = 50;

void setup() {
  size(200, 200);
  smooth(); 
  strokeWeight(20.0);
  stroke(0, 100);
}

void draw() {
  background(226);
  
  float dx = mouseX - x;
  float dy = mouseY - y;
  angle1 = atan2(dy, dx);  
  x = mouseX - (cos(angle1) * segLength);
  y = mouseY - (sin(angle1) * segLength);
 
  segment(x, y, angle1); 
  ellipse(x, y, 20, 20);
}

void segment(float x, float y, float a) {
  pushMatrix();
  translate(x, y);
  rotate(a);
  line(0, 0, segLength, 0);
  popMatrix();
}
