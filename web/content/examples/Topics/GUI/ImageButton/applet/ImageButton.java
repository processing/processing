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

public class ImageButton extends PApplet {

/**
 * Image button. 
 * 
 * Loading images and using them to create a button. 
 */
 

ImageButtons button;

public void setup()
{
  size(200, 200);
  background(102, 102, 102);
  
  // Define and create image button
  PImage b = loadImage("base.gif");
  PImage r = loadImage("roll.gif");
  PImage d = loadImage("down.gif");
  int x = width/2 - b.width/2;
  int y = height/2 - b.height/2; 
  int w = b.width;
  int h = b.height;
  button = new ImageButtons(x, y, w, h, b, r, d);
}

public void draw()
{
  button.update();
  button.display();
}

class Button
{
  int x, y;
  int w, h;
  int basecolor, highlightcolor;
  int currentcolor;
  boolean over = false;
  boolean pressed = false;   
  
  public void pressed() {
    if(over && mousePressed) {
      pressed = true;
    } else {
      pressed = false;
    }    
  }
  
  public boolean overRect(int x, int y, int width, int height) {
  if (mouseX >= x && mouseX <= x+width && 
      mouseY >= y && mouseY <= y+height) {
    return true;
  } else {
    return false;
  }
}
}

class ImageButtons extends Button 
{
  PImage base;
  PImage roll;
  PImage down;
  PImage currentimage;

  ImageButtons(int ix, int iy, int iw, int ih, PImage ibase, PImage iroll, PImage idown) 
  {
    x = ix;
    y = iy;
    w = iw;
    h = ih;
    base = ibase;
    roll = iroll;
    down = idown;
    currentimage = base;
  }
  
  public void update() 
  {
    over();
    pressed();
    if(pressed) {
      currentimage = down;
    } else if (over){
      currentimage = roll;
    } else {
      currentimage = base;
    }
  }
  
  public void over() 
  {
    if( overRect(x, y, w, h) ) {
      over = true;
    } else {
      over = false;
    }
  }
  
  public void display() 
  {
    image(currentimage, x, y);
  }
}





  static public void main(String args[]) {
    PApplet.main(new String[] { "ImageButton" });
  }
}
