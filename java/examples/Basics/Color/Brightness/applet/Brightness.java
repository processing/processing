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

public class Brightness extends PApplet {

/**
 * Brightness 
 * by Rusty Robison. 
 * 
 * Brightness is the relative lightness or darkness of a color.
 * Move the cursor vertically over each bar to alter its brightness. 
 */
 
int barWidth = 5;
int[] brightness;

public void setup() 
{
  size(200, 200);
  colorMode(HSB, 360, height, height);  
  brightness = new int[width/barWidth];
}

public void draw() 
{
  int j = 0;
  for (int i = 0; i <= (width-barWidth); i += barWidth) {  
    noStroke();
    if ((mouseX > i) && (mouseX < i+barWidth)) {
      brightness[j] = mouseY;
    }
    fill(i, height, brightness[j]);
    rect(i, 0, barWidth, height);  
    j++;
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Brightness" });
  }
}
