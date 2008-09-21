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

public class WidthHeight extends PApplet {
  public void setup() {/**
 * Width and Height. 
 * 
 * The 'width' and 'height' variables contain the width and height 
 * of the display window as defined in the size() function. 
 */
 
size(200, 200);
background(127);
noStroke();
for(int i=0; i<height; i+=20) {
  fill(0);
  rect(0, i, width, 10);
  fill(255);
  rect(i, 0, 10, height);
}

  noLoop();
} 
  static public void main(String args[]) {
    PApplet.main(new String[] { "WidthHeight" });
  }
}
