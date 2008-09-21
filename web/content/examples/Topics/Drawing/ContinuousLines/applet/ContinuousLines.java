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

public class ContinuousLines extends PApplet {

/**
 * Continuous Lines. 
 * 
 * Click and drag the mouse to draw a line. 
 */
 
public void setup() {
  size(640, 200);
  background(102);
}

public void draw() {
  stroke(255);
  if(mousePressed) {
    line(mouseX, mouseY, pmouseX, pmouseY);
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "ContinuousLines" });
  }
}
