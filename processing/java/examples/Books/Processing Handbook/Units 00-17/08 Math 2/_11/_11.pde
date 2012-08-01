// Draw a line from the top of the display window to
// points on a curve y = x^4 from x in range -1.0 to 1.0
for (int x = 5; x < 100; x += 5) {
  float n = map(x, 5, 95, -1, 1);
  float p = pow(n, 4);
  float ypos = lerp(20, 80, p);
  line(x, 0, x, ypos);
}