/** 
 * Synthesis 1: Form and Code
 * Riley Waves by Casey Reas (www.processing.org)
 * p. 151
 * 
 * Step 3, values are modified to create a new pattern. 
*/


size(1200, 280);
background(255);
smooth();
noStroke();
fill(0);
float angle = 0.0;
int magnitude = 16;

for(int i = -magnitude; i < width+magnitude; i = i+16) {

  fill(float(i)/width * 255);

  beginShape(TRIANGLE_STRIP);
  for(int y = 0; y <= height; y = y+6) {
    float x = i + (sin(angle)* magnitude);
    angle += PI/22.0;
    float x2 = i+8 + (sin(angle+PI/22)* magnitude);
    vertex(x, y);
    vertex(x2, y);
  }
  endShape();

}

// saveFrame("Synthesis-2--2.tif");
