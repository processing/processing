class Scrollbar {
  int x, y; // The x- and y-coordinates
  float sw, sh; // Width and height of scrollbar
  float pos; // Position of thumb
  float posMin, posMax; // Max and min values of thumb
  boolean rollover; // True when the mouse is over
  boolean locked; // True when its the active scrollbar
  float minVal, maxVal; // Min and max values for the thumb
  Scrollbar(int xp, int yp, int w, int h, float miv, float mav) {
    x = xp;
    y = yp;
    sw = w;
    sh = h;
    minVal = miv;
    maxVal = mav;
    pos = x + sw / 2 - sh / 2;
    posMin = x;
    posMax = x + sw - sh;
  }

  // Updates the over boolean and the position of the thumb
  void update(int mx, int my) {
    if (over(mx, my) == true) {
      rollover = true;
    } else {
      rollover = false;
    }
    if (locked == true) {
      pos = constrain(mx - sh / 2, posMin, posMax);
    }
  }

  // Locks the thumb so the mouse can move off and still update
  void press(int mx, int my) {
    if (rollover == true) {
      locked = true;
    } else {
      locked = false;
    }
  }

  // Resets the scrollbar to neutral
  void release() {
    locked = false;
  }
  
  // Returns true if the cursor is over the scrollbar
  boolean over(int mx, int my) {
    if ((mx > x) && (mx < x + sw) && (my > y) && (my < y + sh)) {
      return true;
    } else {
      return false;
    }
  }

  // Draws the scrollbar to the screen
  void display() {
    fill(255);
    rect(x, y, sw, sh);
    if ((rollover == true) || (locked == true)) {
      fill(0);
    } else {
      fill(102);
    }
    rect(pos, y, sh, sh);
  }

  // Returns the current value of the thumb
  float getPos() {
    float scalar = sw / (sw - sh);
    float ratio = (pos - x) * scalar;
    float offset = minVal + (ratio / sw * (maxVal - minVal));
    return offset;
  }
}
