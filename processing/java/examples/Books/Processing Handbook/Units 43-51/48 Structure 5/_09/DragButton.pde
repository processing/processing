class DragButton extends Button {
  int xoff, yoff;

  DragButton(int x, int y, int s, color bv, color ov, color pv) {
    super(x, y, s, bv, ov, pv);
  }

  void press(int mx, int my) {
    super.press();
    xoff = mx - x;
    yoff = my - y;
  }

  void drag(int mx, int my) {
    if (press == true) {
      x = mx - xoff;
      y = my - yoff;
    }
  }
}
