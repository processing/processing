/**
 * Arm. 
 * 
 * The angle of each segment is controlled with the mouseX and
 * mouseY position. The transformations applied to the first segment
 * are also applied to the second segment because they are inside
 * the same pushMatrix() and popMatrix() group.
*/

float x = 50;
float y = 100;
float angle1 = 0.0;
float angle2 = 0.0;
float segLength = 50;

void setup() {
  size(200, 200);
  smooth(); 
  strokeWeight(20.0);
  stroke(0, 100);
}

void draw() {
  background(226);
  
  angle1 = (mouseX/float(width) - 0.5) * -PI;
  angle2 = (mouseY/float(height) - 0.5) * PI;
  
  pushMatrix();
  segment(x, y, angle1); 
  segment(segLength, 0, angle2);
  popMatrix();
}

void segment(float x, float y, float a) {
  translate(x, y);
  rotate(a);
  line(0, 0, segLength, 0);
}
