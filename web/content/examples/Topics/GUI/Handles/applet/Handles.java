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

public class Handles extends PApplet {

/**
 * Handles. 
 * 
 * Click and drag the white boxes to change their position. 
 */
 
Handle[] handles;
int num;

public void setup()
{
  size(200, 200);
  num = height/15;
  handles = new Handle[num];
  int hsize = 10;
  for(int i=0; i<num; i++) {
    handles[i] = new Handle(width/2, 10+i*15, 50-hsize/2, 10, handles);
  }
}

public void draw()
{
  background(153);
  
  for(int i=0; i<num; i++) {
    handles[i].update();
    handles[i].display();
  }
  
  fill(0);
  rect(0, 0, width/2, height);
}

public void mouseReleased() 
{
  for(int i=0; i<num; i++) {
    handles[i].release();
  }
}

class Handle
{
  int x, y;
  int boxx, boxy;
  int length;
  int size;
  boolean over;
  boolean press;
  boolean locked = false;
  boolean otherslocked = false;
  Handle[] others;
  
  Handle(int ix, int iy, int il, int is, Handle[] o)
  {
    x = ix;
    y = iy;
    length = il;
    size = is;
    boxx = x+length - size/2;
    boxy = y - size/2;
    others = o;
  }
  
  public void update() 
  {
    boxx = x+length;
    boxy = y - size/2;
    
    for(int i=0; i<others.length; i++) {
      if(others[i].locked == true) {
        otherslocked = true;
        break;
      } else {
        otherslocked = false;
      }  
    }
    
    if(otherslocked == false) {
      over();
      press();
    }
    
    if(press) {
      length = lock(mouseX-width/2-size/2, 0, width/2-size-1);
    }
  }
  
  public void over()
  {
    if(overRect(boxx, boxy, size, size)) {
      over = true;
    } else {
      over = false;
    }
  }
  
  public void press()
  {
    if(over && mousePressed || locked) {
      press = true;
      locked = true;
    } else {
      press = false;
    }
  }
  
  public void release()
  {
    locked = false;
  }
  
  public void display() 
  {
    line(x, y, x+length, y);
    fill(255);
    stroke(0);
    rect(boxx, boxy, size, size);
    if(over || press) {
      line(boxx, boxy, boxx+size, boxy+size);
      line(boxx, boxy+size, boxx+size, boxy);
    }

  }
}

public boolean overRect(int x, int y, int width, int height) 
{
  if (mouseX >= x && mouseX <= x+width && 
      mouseY >= y && mouseY <= y+height) {
    return true;
  } else {
    return false;
  }
}

public int lock(int val, int minv, int maxv) 
{ 
  return  min(max(val, minv), maxv); 
} 

  static public void main(String args[]) {
    PApplet.main(new String[] { "Handles" });
  }
}
