noStroke();
for (int y=0; y<100; y+=10) {
  for (int x=0; x<100; x+=10) {
    fill((x+y) * 1.4);
    rect(x, y, 10, 10);
  }
}
