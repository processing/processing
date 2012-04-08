
class Slider extends Control {
  float min,max,value;
  int labelW = 8;

  Slider(int x, int y, int w, int h, float min, float max, float value,String label) {
    super(x,y,w,h,label);
    this.min = min;
    this.max = max;
    this.value = value;
  }

  void mouseDragged() {
    setValueToMouse();
  }

  boolean mousePressed() {
    boolean down;
    if (down = super.mousePressed())
    setValueToMouse();
    return down;
  }

  void setValueToMouse() {
    int mw = (w-labelW)-1;
    float mv = (mouseX-(x+labelW+1.0))/mw;
    if (mv>0)
    value = min+(mv*mv)*(max-min);
    else
    value = min;
    value = min(value,max);
  }

  void drawContents() {
    fill(0x00,0x99,0xFF);
    noStroke();
    int mw = (w-labelW)-1;
    float vw = sqrt((value-min)/(max-min))*mw;
    rect(x+labelW+1,y+1,vw,h-1);
  }

  void display() {
    super.display();
    stroke(0x00,0x99,0xFF);
    line(x+labelW,y,x+labelW,y+h);
  }
}

