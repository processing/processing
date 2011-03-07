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

public class ArrayObjects extends PApplet {

/**
 * Array Objects. 
 * 
 * Demonstrates the syntax for creating an array of custom objects. 
 */ 
 
int unit = 40;
int num;
Module[] mods;

public void setup() 
{
  size(200, 200);
  background(176);
  noStroke();
  
  num = width/unit * width/unit;
  mods = new Module[num];
  
  for (int i=0; i<height/unit; i++) {
    for(int j=0; j<height/unit; j++) {
      int index = i*height/unit + j;
      mods[index] = new Module(j*unit, i*unit, unit/2, unit/2, random(0.05f, 0.8f));  
    }
  }
}

public void draw() 
{
  for(int i=0; i<num; i++) {
    mods[i].update();
    mods[i].draw();
  }
}

class Module {
  float mx, my;
  int size = unit;
  float x, y = 0;
  int xdir = 1;
  int ydir = 1;
  float speed; 
  
  // Contructor (required)
  Module(float imx, float imy, float ix, float iy, float ispeed) {
    mx = imy;
    my = imx;
    x = PApplet.parseInt(ix);
    y = PApplet.parseInt(iy);
    speed = ispeed;
  }
  
  // Custom method for updating the variables
  public void update() {
    x = x + (speed * xdir);
    if (x >= size || x <= 0) {
      xdir *= -1;
      x = x + (1 * xdir);
      y = y + (1 * ydir);
    }
    if (y >= size || y <= 0) {
      ydir *= -1;
      y = y + (1 * ydir);
    }
  }
  
  // Custom method for drawing the object
  public void draw() {
    stroke(second()*4);
    point(mx+x-1, my+y-1);
  }
}



  static public void main(String args[]) {
    PApplet.main(new String[] { "ArrayObjects" });
  }
}
