/**
 * TRIANGLE_STRIP Mode
 * by Ira Greenberg. 
 * 
 * Generate a closed ring using vertex() 
 * function and beginShape(TRIANGLE_STRIP)
 * mode. outerRad and innerRad variables 
 * control ring's outer/inner radii respectively.
 * Trig functions generate ring.
 */

int x;
int y;
float outerRad;
float innerRad;
float px = 0; 
float py = 0; 
float angle = 0;
float pts = 36;
float rot = 360.0/pts;

void setup() {
  size(640, 360);
  background(204);
  smooth();
  x = width/2;
  y = height/2;
  outerRad = min(width, height) * 0.4;
  innerRad = outerRad * 0.6;
}

void draw() {
  beginShape(TRIANGLE_STRIP); 
  for (int i = 0; i < pts; i++) {
    px = x + cos(radians(angle)) * outerRad;
    py = y + sin(radians(angle)) * outerRad;
    angle += rot;
    vertex(px, py);
    px = x + cos(radians(angle)) * innerRad;
    py = y + sin(radians(angle)) * innerRad;
    vertex(px, py); 
    angle += rot;
  }
  endShape();
}

