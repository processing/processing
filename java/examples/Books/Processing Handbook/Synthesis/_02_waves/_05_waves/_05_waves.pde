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
//fill(0);
float angle = PI;
float angle2 = PI;
int magnitude = 3;

for(int i = -magnitude; i < height+magnitude; i = i+12) {

  angle2 = angle;
  
  fill(0);
  beginShape(TRIANGLE_STRIP);
  for(int x = 0; x <= width; x = x+8) {
    float y = i + (sin(angle)* magnitude);
    angle += PI/24.0;
    float y2 = i+4 + (sin(angle+PI/12)* magnitude);
    vertex(x, y);
    vertex(x, y2);
  }
  endShape();

}


// save("Synthesis-2--3.tif");
