/** 
 * Synthesis 1: Form and Code
 * Riley Waves by Casey Reas (www.processing.org)
 * p. 151
 * 
 * Step 1, creating the basic form. 
*/


size(400, 400);
background(255);
float angle = 0.0;
float magnitude = 24.0;

beginShape(TRIANGLE_STRIP);
for(int x=0; x<=width; x=x+8) {
  float y = 50 + (sin(angle)* magnitude);
  angle += PI/48.0;
  float y2 = 70 + (sin(angle+PI/6)* magnitude);
  vertex(x, y);
  vertex(x, y2);
}
endShape();
