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

public class PointsLines extends PApplet {
  public void setup() {/**
 * Points and Lines. 
 * 
 * Constructing a simple dimensional form with lines and rectangles.
 * Changing the value of the variable 'd' scales the image.
 * The four variables set the positions based on the value of 'd'. 
 */
 
int d = 40;
int p1 = d;
int p2 = p1+d;
int p3 = p2+d;
int p4 = p3+d;

size(200, 200);
background(0);

// Draw gray box
stroke(153);
line(p3, p3, p2, p3);
line(p2, p3, p2, p2);
line(p2, p2, p3, p2);
line(p3, p2, p3, p3);

// Draw white points
stroke(255);
point(p1, p1);
point(p1, p3); 
point(p2, p4);
point(p3, p1); 
point(p4, p2);
point(p4, p4);

  noLoop();
} 
  static public void main(String args[]) {
    PApplet.main(new String[] { "PointsLines" });
  }
}
