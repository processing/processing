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

public class Mouse1D extends PApplet {

/**
 * Mouse 1D. 
 * 
 * Move the mouse left and right to shift the balance. 
 * The "mouseX" variable is used to control both the 
 * size and color of the rectangles. 
 */
 
int gx = 15;
int gy = 35;
float leftColor = 0.0f;
float rightColor = 0.0f;

public void setup() 
{
  size(200, 200);
  colorMode(RGB, 1.0f);
  noStroke();
}

public void draw() 
{
  background(0.0f);
  update(mouseX); 
  fill(0.0f, leftColor + 0.4f, leftColor + 0.6f); 
  rect(width/4-gx, width/2-gx, gx*2, gx*2); 
  fill(0.0f, rightColor + 0.2f, rightColor + 0.4f); 
  rect(width/1.33f-gy, width/2-gy, gy*2, gy*2);
}

public void update(int x) 
{
  leftColor = -0.002f * x/2 + 0.06f;
  rightColor =  0.002f * x/2 + 0.06f;
	
  gx = x/2;
  gy = 100-x/2;

  if (gx < 10) {
    gx = 10;
  } else if (gx > 90) {
    gx = 90;
  }

  if (gy > 90) {
    gy = 90;
  } else if (gy < 10) {
    gy = 10;
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Mouse1D" });
  }
}
