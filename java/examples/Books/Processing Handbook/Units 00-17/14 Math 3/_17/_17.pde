smooth();
float radius = 0.15;
float cx = 33; // Center x- and y-coordinates
float cy = 66;
float px = cx; // Start with center as the
float py = cy; // previous coordinate
for (int deg = 0; deg < 360*5; deg += 12) {
  float angle = radians(deg);
  float x = cx + (cos(angle) * radius);
  float y = cy + (sin(angle) * radius);
  line(px, py, x, y);
  radius = radius * 1.045;
  px = x;
  py = y;
}
