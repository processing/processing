
class Button extends Control {
  boolean selected = false;
  boolean down = false;
  Button(int x, int y, int w, int h, String label) {
    super(x,y,w,h,label);
  }

  boolean mousePressed() {
    if (super.mousePressed() && !selected)
    down = true;
    return down;
  }

  boolean mouseReleased() {
    down = false;
    if (super.mouseIn()) {
      selected = !selected;
      return true;
    }
    return false;
  }

  void drawContents() {
    if (selected||down) {
      if (!selected&&(over^down))
      fill(0xDD,0xDD,0xDD);
      else
      fill(0x00,0x99,0xFF);
      noStroke();
      rect(x+1,y+1,w-1,h-1);
    }
  }
}

