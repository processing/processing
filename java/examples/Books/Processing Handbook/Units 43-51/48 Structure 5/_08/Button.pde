class Button {
  int x, y; // x-coordinate, y-coordinate of the rectangle
  int size; // Dimension (width and height) of the rectangle
  color baseGray; // Default gray value
  color overGray; // Gray value when mouse is over the button
  color pressGray; // Gray value when mouse is over and pressed
  boolean over = false; // True when the mouse is over
  boolean press = false; // True when the mouse is over and pressed

  Button(int xpos, int ypos, int s, color b, color o, color p) {
    x = xpos;
    y = ypos;
    size = s;
    baseGray = b;
    overGray = o;
    pressGray = p;
  }
  
  // Updates the over field every frame
  void update(int mx, int my) {
    if ((mx >= x) && (mx <= x+size) &&
        (my >= y) && (my <= y+size)) {
      over = true;
    } 
    else {
      over = false;
    }
  }
  
  // Updates the press boolean when mouse is pressed
  boolean press() {
    if (over == true) {
      press = true;
      return true;
    } 
    else {
      return false;
    }
  }
  
  // Sets the press boolean to false when mouse is released
  void release() {
    press = false;
  }
  
  // Draws the button
  void display() {
    if (press == true) {
      fill(pressGray);
    } else if (over == true) {
      fill(overGray);
    } else {
      fill(baseGray);
    }
    stroke(255);
    rect(x, y, size, size);
  }
}

