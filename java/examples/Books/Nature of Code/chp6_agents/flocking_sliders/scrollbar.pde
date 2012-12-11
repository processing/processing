// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Code based on "Scrollbar" by Casey Reas

HScrollbar[] hs = new HScrollbar[5];//
String[] labels =  {"separation", "alignment","cohesion","maxspeed","maxforce"};

int x = 5;
int y = 20;
int w = 50;
int h = 8;
int l = 2;
int spacing = 4;

void setupScrollbars() {
  for (int i = 0; i < hs.length; i++) {
    hs[i] = new HScrollbar(x, y + i*(h+spacing), w, h, l);
  }

  hs[0].setPos(0.5);
  hs[1].setPos(0.5);
  hs[2].setPos(0.5);
  hs[3].setPos(0.5);
  hs[4].setPos(0.05);

}

void drawScrollbars() {
  //if (showvalues) {
  swt = hs[0].getPos()*10.0f;     //sep.mult(25.0f);
  awt = hs[1].getPos()*2.0f;     //sep.mult(25.0f);
  cwt = hs[2].getPos()*2.0f;     //sep.mult(25.0f);
  maxspeed = hs[3].getPos()*10.0f;
  maxforce = hs[4].getPos()*0.5;


  if (showvalues) {
    for (int i = 0; i < hs.length; i++) {
      hs[i].update();
      hs[i].draw();
      fill(0);
      textAlign(LEFT);
      text(labels[i],x+w+spacing,y+i*(h+spacing)+spacing);
      //text(hs[i].getPos(),x+w+spacing+75,y+i*(h+spacing)+spacing);
    }
  }
}


class HScrollbar
{
  int swidth, sheight;    // width and height of bar
  int xpos, ypos;         // x and y position of bar
  float spos, newspos;    // x position of slider
  int sposMin, sposMax;   // max and min values of slider
  int loose;              // how loose/heavy
  boolean over;           // is the mouse over the slider?
  boolean locked;
  float ratio;

  HScrollbar (int xp, int yp, int sw, int sh, int l) {
    swidth = sw;
    sheight = sh;
    int widthtoheight = sw - sh;
    ratio = (float)sw / (float)widthtoheight;
    xpos = xp;
    ypos = yp-sheight/2;
    spos = xpos;
    newspos = spos;
    sposMin = xpos;
    sposMax = xpos + swidth - sheight;
    loose = l;
  }

  void update() {
    if(over()) {
      over = true;
    } 
    else {
      over = false;
    }
    if(mousePressed && over) {
      scrollbar = true;
      locked = true;
    }
    if(!mousePressed) {
      locked = false;
      scrollbar = false;
    }
    if(locked) {
      newspos = constrain(mouseX-sheight/2, sposMin, sposMax);
    }
    if(abs(newspos - spos) > 0) {
      spos = spos + (newspos-spos)/loose;
    }
  }

  int constrain(int val, int minv, int maxv) {
    return min(max(val, minv), maxv);
  }

  boolean over() {
    if(mouseX > xpos && mouseX < xpos+swidth &&
      mouseY > ypos && mouseY < ypos+sheight) {
      return true;
    } 
    else {
      return false;
    }
  }

  void draw() {
    fill(255);
    rectMode(CORNER);
    rect(xpos, ypos, swidth, sheight);
    if(over || locked) {
      fill(153, 102, 0);
    } 
    else {
      fill(102, 102, 102);
    }
    rect(spos, ypos, sheight, sheight);
  }

  void setPos(float s) {
    spos = xpos + s*(sposMax-sposMin);
    newspos = spos;
  }

  float getPos() {
    // convert spos to be values between
    // 0 and the total width of the scrollbar
    return ((spos-xpos))/(sposMax-sposMin);// * ratio;
  }
}
