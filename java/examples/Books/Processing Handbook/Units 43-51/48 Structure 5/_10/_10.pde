class DragImage extends DragButton {
  PImage img;
  
  DragImage(int x, int y, int d, String s) {
    super(x, y, d, color(204), color(255), color(0));
    img = loadImage(s);
  }

  // Override the display() from Button
  void display() {
    if (press == true) {
      stroke(pressGray);
    } else if (over == true) {
      stroke(overGray);
    } else {
      stroke(baseGray);
    }
    noFill();
    rect(x - 1, y - 1, size + 1, size + 1);
    image(img, x, y, size, size);
  }
}
