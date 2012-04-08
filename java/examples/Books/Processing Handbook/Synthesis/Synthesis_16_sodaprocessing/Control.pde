
class Control {
  int x,y,w,h;
  boolean over = false;
  String label;

  Control(int x, int y, int w, int h, String label) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.label = label;
  }

  boolean mouseIn() {
    return over = mouseX>x&&mouseX<x+w&&mouseY>y&&mouseY<y+h;
  }

  boolean mousePressed() {
    return mouseIn();
  }

  void mouseDragged() {
    mouseIn();
  }

  boolean mouseReleased() {
    return mouseIn();
  }

  void display() {
    stroke(0x00,0x99,0xFF);
    if (over)
    fill(0xDD,0xDD,0xDD);
    else
    fill(0xFF,0xFF,0xFF);
    rect(x,y,w,h);
    drawContents();
    stroke(0);
    fill(0);
    text(label, x+2, (y+h)-3);
  }

  void drawContents() {

  }
}

