void update(int mx, int my) {
  if (dist(mx, my, x, y) < diameter / 2) {
    if (gray < 250) {
      gray++;
    }
  } else {
    if (gray > 0.0) {
      gray--;
    }
  }
}
