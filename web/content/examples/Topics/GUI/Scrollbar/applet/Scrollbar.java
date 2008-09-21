import processing.core.*; 

import java.applet.*; 
import java.awt.*; 
import java.awt.image.*; 
import java.awt.event.*; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class Scrollbar extends PApplet {

/**
 * Scrollbar. 
 * 
 * Move the scrollbars left and right to change the positions of the images. 
 */
 
HScrollbar hs1, hs2;

PImage top, bottom;         // Two image to load
int topWidth, bottomWidth;  // The width of the top and bottom images


public void setup()
{
  size(200, 200);
  noStroke();
  hs1 = new HScrollbar(0, 20, width, 10, 3*5+1);
  hs2 = new HScrollbar(0, height-20, width, 10, 3*5+1);
  top = loadImage("seedTop.jpg");
  topWidth = top.width;
  bottom = loadImage("seedBottom.jpg");
  bottomWidth = bottom.width;
}

public void draw()
{
  background(255);
  
  // Get the position of the top scrollbar
  // and convert to a value to display the top image 
  float topPos = hs1.getPos()-width/2;
  fill(255);
  image(top, width/2-topWidth/2 + topPos*2, 0);
  
  // Get the position of the bottom scrollbar
  // and convert to a value to display the bottom image
  float bottomPos = hs2.getPos()-width/2;
  fill(255);
  image(bottom, width/2-bottomWidth/2 + bottomPos*2, height/2);
 
  hs1.update();
  hs2.update();
  hs1.display();
  hs2.display();
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
    spos = xpos + swidth/2 - sheight/2;
    newspos = spos;
    sposMin = xpos;
    sposMax = xpos + swidth - sheight;
    loose = l;
  }

  public void update() {
    if(over()) {
      over = true;
    } else {
      over = false;
    }
    if(mousePressed && over) {
      locked = true;
    }
    if(!mousePressed) {
      locked = false;
    }
    if(locked) {
      newspos = constrain(mouseX-sheight/2, sposMin, sposMax);
    }
    if(abs(newspos - spos) > 1) {
      spos = spos + (newspos-spos)/loose;
    }
  }

  public int constrain(int val, int minv, int maxv) {
    return min(max(val, minv), maxv);
  }

  public boolean over() {
    if(mouseX > xpos && mouseX < xpos+swidth &&
    mouseY > ypos && mouseY < ypos+sheight) {
      return true;
    } else {
      return false;
    }
  }

  public void display() {
    fill(255);
    rect(xpos, ypos, swidth, sheight);
    if(over || locked) {
      fill(153, 102, 0);
    } else {
      fill(102, 102, 102);
    }
    rect(spos, ypos, sheight, sheight);
  }

  public float getPos() {
    // Convert spos to be values between
    // 0 and the total width of the scrollbar
    return spos * ratio;
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Scrollbar" });
  }
}
