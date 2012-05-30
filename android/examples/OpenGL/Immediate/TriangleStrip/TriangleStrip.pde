/**
 * TRIANGLE_STRIP Mode
 * by Ira Greenberg. 
 * 
 * Generate a closed ring using the vertex() function and 
 * beginShape(TRIANGLE_STRIP) mode. The outerRad and innerRad 
 * variables control ring's radii respectively.
 */

int x;
int y;
float outerRad;
float innerRad;

void setup() {
  size(640, 360, P2D);
  orientation(LANDSCAPE);
  background(204);
  x = width/2;
  y = height/2;
  outerRad = min(width, height) * 0.4;
  innerRad = outerRad * 0.6;
}

void draw() {
  background(204);
  
  int pts = int(map(mouseX, 0, width, 6, 60));
  float rot = 180.0/pts;
  float angle = 0;
  
  beginShape(TRIANGLE_STRIP); 
  for (int i = 0; i <= pts; i++) {
    float px = x + cos(radians(angle)) * outerRad;
    float py = y + sin(radians(angle)) * outerRad;
    angle += rot;
    vertex(px, py);
    px = x + cos(radians(angle)) * innerRad;
    py = y + sin(radians(angle)) * innerRad;
    vertex(px, py); 
    angle += rot;
  }
  endShape();
}

