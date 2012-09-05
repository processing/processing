/**
 * On/Off.  
 * 
 * Uses the default lights to show a simple box. The lights() function
 * is used to turn on the default lighting. Click the mouse to turn the
 * lights off.
 */
 
float spin = 0.0;
boolean on = true;

void setup() {
  size(640, 360, P3D);
  orientation(LANDSCAPE);
  noStroke();
}

void draw() {
  background(51);
  
  if (on) {
    lights();
  }
  
  spin += 0.01;
  
  pushMatrix();
  translate(width/2, height/2, 0);
  rotateX(PI/9);
  rotateY(PI/5 + spin);
  box(150);
  popMatrix();
}

void mousePressed() {
  on = !on; 
}
