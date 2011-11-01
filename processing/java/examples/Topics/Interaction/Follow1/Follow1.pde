/**
 * Follow 1  
 * based on code from Keith Peters. 
 * 
 * A line segment is pushed and pulled by the cursor.
 */

float x = 100;
float y = 100;
float angle1 = 0.0;
float segLength = 50;

void setup() {
  size(640, 360);
  strokeWeight(20.0);
  stroke(255, 100);
}

void draw() {
  background(0);
  
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
