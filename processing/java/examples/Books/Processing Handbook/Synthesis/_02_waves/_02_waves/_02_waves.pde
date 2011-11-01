/** 
 * Synthesis 1: Form and Code
 * Riley Waves by Casey Reas (www.processing.org)
 * p. 151
 * 
 * Step 2, the basic form is drawn many times inside a for structure. 
*/


size(400, 400);
background(255);
float angle = 0.0;
int magnitude = 18;

for(int i =- magnitude; i < height+magnitude; i = i+24) {

  beginShape(TRIANGLE_STRIP);
  for(int x = 0; x <= width; x = x+8) {
    float y = i + (sin(angle)* magnitude);
    angle += PI/24.0;
    float y2 = i+10 + (sin(angle+PI/12) * magnitude);
    vertex(x, y);
    vertex(x, y2);
  }
  endShape();

}
