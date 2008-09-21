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

public class Hue extends PApplet {

/**
 * Hue. 
 * 
 * Hue is the color reflected from or transmitted through an object 
 * and is typically referred to as the name of the color (red, blue, yellow, etc.) 
 * Move the cursor vertically over each bar to alter its hue. 
 */
 
int barWidth = 5;
int[] hue;

public void setup() 
{
  size(400, 400);
  colorMode(HSB, 360, height, height);  
  hue = new int[width/barWidth];
  noStroke();
}

public void draw() 
{
  int j = 0;
  for (int i=0; i<=(width-barWidth); i+=barWidth) {  
    if ((mouseX > i) && (mouseX < i+barWidth)) {
      hue[j] = mouseY;
    }
    fill(hue[j], height/1.2f, height/1.2f);
    rect(i, 0, barWidth, height);  
    j++;
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Hue" });
  }
}
