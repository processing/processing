size(700, 100);
float offset = 50;
float scaleVal = 30.0;
float angleInc = PI/56.0;
float angle = 0.0;
beginShape(TRIANGLE_STRIP);
for (int x = 4 ; x <= width+5; x += 5) {
  float y = sin(angle) * scaleVal;
  if ((x % 2) == 0) { // Every other time through the loop
    vertex(x, offset + y);
  } else {
    vertex(x, offset - y);
  }
  angle += angleInc;
}
endShape();