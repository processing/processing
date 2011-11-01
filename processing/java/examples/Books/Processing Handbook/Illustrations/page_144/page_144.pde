
// Based on code 14-11 (p. 123)


size(440, 666);
//size(440, 666, PDF, "page_144.pdf");
background(255);
stroke(0);
strokeWeight(0.25);
fill(0);

float scaleVal = 16.0;
float angleInc = PI/24.0;
float angle = 0.0;

for (int offset = 20; offset < width-20; offset += 5) {
  for (int y = 0; y <= height; y += 2) {
    float x = offset + (sin(angle) * scaleVal);
    line(x, y, x+0.125, y+0.125);
    angle += angleInc;	
  }
  angle += PI;
}
