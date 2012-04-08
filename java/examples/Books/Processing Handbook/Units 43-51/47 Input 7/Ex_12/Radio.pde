class Radio {
  int x, y; // The x- and y-coordinates of the rect
  int size, dotSize; // Dimension of circle, inner circle
  color baseGray, dotGray; // Circle gray value, inner gray value
  boolean checked = false; // True when the button is selected
  int me; // ID number for this Radio object
  Radio[] others; // Array of all other Radio objects

  Radio(int xp, int yp, int s, color b, color d, int m, Radio[] o) {
    x = xp;
    y = yp;
    size = s;
    dotSize = size - size / 3;
    ;
    baseGray = b;
    dotGray = d;
    others = o;
    me = m;
  }
  
  // Updates the boolean value press, returns true or false
  boolean press(float mx, float my) {
    if (dist(x, y, mx, my) < size / 2) {
      checked = true;
      for (int i = 0; i < others.length; i++) {
        if (i != me) {
          others[i].checked = false;
        }
      }
      return true;
    } else {
      return false;
    }
  }

  // Draws the element to the display window
  void display() {
    noStroke();
    fill(baseGray);
    ellipse(x, y, size, size);
    if (checked == true) {
      fill(dotGray);
      ellipse(x, y, dotSize, dotSize);
    }
  }
}
