background(56, 90, 94);
smooth();
int x = 0;
strokeWeight(12);
for (int i = 51; i <= 255; i += 51) {
  stroke(242, 204, 47, i);
  line(x, 20, x+20, 80);
  x += 20;
}