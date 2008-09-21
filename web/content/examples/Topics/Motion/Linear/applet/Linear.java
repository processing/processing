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

public class Linear extends PApplet {

/**
 * Linear Motion. 
 * 
 * Changing a variable to create a moving line.  
 * When the line moves off the edge of the window, 
 * the variable is set to 0, which places the line
 * back at the bottom of the screen. 
 */
 
float a = 100;

public void setup() 
{
  size(640, 360);
  stroke(255);
}

public void draw() 
{
  background(51);
  a = a - 0.5f;
  if (a < 0) { 
    a = height; 
  }
  line(0, a, width, a);  
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Linear" });
  }
}
