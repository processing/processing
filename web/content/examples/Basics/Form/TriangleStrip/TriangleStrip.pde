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

size(200, 200);
background(204);
smooth();

int x = width/2;
int y = height/2;
float outerRad = min(width, height) * 0.4;
float innerRad = outerRad * 0.6;
float px = 0, py = 0, angle = 0;
float pts = 36;
float rot = 360.0/pts;

beginShape(TRIANGLE_STRIP); 
for (int i = 0; i < pts; i++) {
  px = x + cos(radians(angle))*outerRad;
  py = y + sin(radians(angle))*outerRad;
  angle += rot;
  vertex(px, py);
  px = x + cos(radians(angle))*innerRad;
  py = y + sin(radians(angle))*innerRad;
  vertex(px, py); 
  angle += rot;
}
endShape();

