class OverCircle {
  int x, y; // The x- and y-coordinates
  int diameter; // Diameter of the circle
  int gray; // Gray value

  OverCircle(int xp, int yp, int d) {
    x = xp;
    y = yp;
    diameter = d;
    gray = 0;
  }

  void update(int mx, int my) {
    if (dist(mx, my, x, y) < diameter / 2) {
      gray = 255;
    } else {
      gray = 0;
    }
  }

  void display() {
    fill(gray);
    ellipse(x, y, diameter, diameter);
  }
}
