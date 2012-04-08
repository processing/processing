class OverRect {
  int x, y; // The x- and y-coordinates
  int size; // Dimension (width and height) of the rectangle
  int gray; // Gray value

  OverRect(int xp, int yp, int s) {
    x = xp;
    y = yp;
    size = s;
    gray = 0;
  }

  void update(int mx, int my) {
    if ((mx > x) && (mx < x + size) && (my > y) && (my < y + size)) {
      gray = 255;
    } else {
      gray = 0;
    }
  }

  void display() {
    fill(gray);
    rect(x, y, size, size);
  }
}
