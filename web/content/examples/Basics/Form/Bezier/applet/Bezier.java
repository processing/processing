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

public class Bezier extends PApplet {
  public void setup() {/**
 * Bezier. 
 * 
 * The first two parameters for the bezier() function specify the 
 * first point in the curve and the last two parameters specify 
 * the last point. The middle parameters set the control points
 * that define the shape of the curve. 
 */
 
size(200, 200); 
background(0); 
stroke(255);
noFill();
smooth(); 

for(int i = 0; i < 100; i += 20) {
  bezier(90-(i/2.0f), 20+i, 210, 10, 220, 150, 120-(i/8.0f), 150+(i/4.0f));
}

  noLoop();
} 
  static public void main(String args[]) {
    PApplet.main(new String[] { "Bezier" });
  }
}
