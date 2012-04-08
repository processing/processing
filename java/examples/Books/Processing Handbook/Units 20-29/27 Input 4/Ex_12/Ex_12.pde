// Rotate the triangles so they always point
// to the cursor
float x = 50;
float y1 = 33;
float y2 = 66;

void setup() {
  size(100, 100);
  noStroke();
  smooth();
}

void draw() {
  background(0);
  
  // Top triangle
  float angle = atan2(mouseY-y1, mouseX-x);
  pushMatrix();
  translate(x, y1);
  rotate(angle);
  triangle(-20, -8, 20, 0, -20, 8);
  popMatrix();
  pushMatrix();
  
  // Bottom triangle
  float angle2 = atan2(mouseY-(y2), mouseX-x);
  translate(x, y2);
  rotate(angle2);
  triangle(-20, -8, 20, 0, -20, 8);
  popMatrix();
}