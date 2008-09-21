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

public class MouseSignals extends PApplet {

/**
 * Mouse Signals. 
 * 
 * Move and click the mouse to generate signals. 
 * The top row is the signal from "mouseX", 
 * the middle row is the signal from "mouseY",
 * and the bottom row is the signal from "mousePressed". 
 */
 
int[] xvals;
int[] yvals;
int[] bvals;

public void setup() 
{
  size(200, 200);
  xvals = new int[width];
  yvals = new int[width];
  bvals = new int[width];
}

int arrayindex = 0;

public void draw()
{
  background(102);
  
  for(int i=1; i<width; i++) { 
    xvals[i-1] = xvals[i]; 
    yvals[i-1] = yvals[i];
    bvals[i-1] = bvals[i];
  } 
  // Add the new values to the end of the array 
  xvals[width-1] = mouseX; 
  yvals[width-1] = mouseY;
  if(mousePressed) {
    bvals[width-1] = 0;
  } else {
    bvals[width-1] = 255;
  }
  
  fill(255);
  noStroke();
  rect(0, height/3, width, height/3+1);

  for(int i=1; i<width; i++) {
    stroke(255);
    point(i, xvals[i]/3);
    stroke(0);
    point(i, height/3+yvals[i]/3);
    stroke(255);
    line(i, 2*height/3+bvals[i]/3, i, (2*height/3+bvals[i-1]/3));
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "MouseSignals" });
  }
}
