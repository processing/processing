size(700, 100);
noStroke();
smooth();
fill(0);
float offset = 50.0; // Y offset
float scaleVal = 35.0; // Scale value for the wave magnitude
float angleInc = PI/28.0; // Increment between the next angle
float angle = 0.0; // Angle to receive sine values from
for (int x = 0; x <= width; x += 5) {
  float y = offset + (sin(angle) * scaleVal);
  rect(x, y, 2, 4);
  angle += angleInc;
}