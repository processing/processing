import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Saturation extends PApplet {// Saturation
// by REAS <http://reas.com>
// Modified from code by Rusty Robison

// Saturation is the strength or purity of the color and represents the 
// amount of gray in proportion to the hue. A "saturated" color is pure 
// and an "unsaturated" color has a large percentage of gray. 
// Move the cursor vertically over each bar to alter its saturation.

// Updated 26 October 2002

int barWidth = 5;
int[] saturation = new int[200/barWidth];

public void setup() 
{
  size(200, 200);
  colorMode(HSB, 360, height, height);  
}

public void draw() 
{
  int j = 0;
  for (int i=0; i<=(width-barWidth); i+=barWidth) {  
    noStroke();
    if ((mouseX > i) && (mouseX < i+barWidth)) {
      saturation[j] = mouseY;
    }
    fill(i, saturation[j], height/1.5f);
    rect(i, 0, barWidth, height);  
    j++;
  }
}
}