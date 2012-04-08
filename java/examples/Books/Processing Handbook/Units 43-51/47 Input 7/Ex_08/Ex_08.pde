class Check {
  int x, y; // The x- and y-coordinates
  int size; // Dimension (width and height)
  color baseGray; // Default gray value
  boolean checked = false; // True when the check box is selected

  Check(int xp, int yp, int s, color b) {
    x = xp;
    y = yp;
    size = s;
    baseGray = b;
  }

  // Updates the boolean variable checked
  void press(float mx, float my) {
    if ((mx >= x) && (mx <= x + size) && (my >= y) && (my <= y + size)) {
      checked = !checked; // Toggle the check box on and off
    }
  }

  // Draws the box and an X inside if the checked variable is true
  void display() {
    stroke(255);
    fill(baseGray);
    rect(x, y, size, size);
    if (checked == true) {
      line(x, y, x + size, y + size);
      line(x + size, y, x, y + size);
    }
  }
}
