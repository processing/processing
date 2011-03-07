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

public class MouseFunctions extends PApplet {

/**
 * Mouse Functions. 
 * 
 * Click on the box and drag it across the screen. 
 */
 
float bx;
float by;
int bs = 20;
boolean bover = false;
boolean locked = false;
float bdifx = 0.0f; 
float bdify = 0.0f; 


public void setup() 
{
  size(200, 200);
  bx = width/2.0f;
  by = height/2.0f;
  rectMode(CENTER_RADIUS);  
}

public void draw() 
{ 
  background(0);
  
  // Test if the cursor is over the box 
  if (mouseX > bx-bs && mouseX < bx+bs && 
      mouseY > by-bs && mouseY < by+bs) {
    bover = true;  
    if(!locked) { 
      stroke(255); 
      fill(153);
    } 
  } else {
    stroke(153);
    fill(153);
    bover = false;
  }
  
  // Draw the box
  rect(bx, by, bs, bs);
}

public void mousePressed() {
  if(bover) { 
    locked = true; 
    fill(255, 255, 255);
  } else {
    locked = false;
  }
  bdifx = mouseX-bx; 
  bdify = mouseY-by; 

}

public void mouseDragged() {
  if(locked) {
    bx = mouseX-bdifx; 
    by = mouseY-bdify; 
  }
}

public void mouseReleased() {
  locked = false;
}


  static public void main(String args[]) {
    PApplet.main(new String[] { "MouseFunctions" });
  }
}
