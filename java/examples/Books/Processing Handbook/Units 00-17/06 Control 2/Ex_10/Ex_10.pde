for (int y=1; y<100; y+=10) {
  for (int x=1; x<y; x+=10) {
    line(x, y, x+6, y+6);
    line(x+6, y, x, y+6);
  }
}
