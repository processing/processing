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

public class Transparency extends PApplet {

/**
 * Transparency. 
 * 
 * Move the pointer left and right across the image to change
 * its position. This program overlays one image over another 
 * by modifying the alpha value of the image with the tint() function. 
 */

PImage a, b;
float offset;

public void setup() {
  size(200, 200);
  a = loadImage("construct.jpg");  // Load an image into the program 
  b = loadImage("wash.jpg");   // Load an image into the program 
  frameRate(60);
}

public void draw() { 
  image(a, 0, 0);
  float offsetTarget = map(mouseX, 0, width, -b.width/2 - width/2, 0);
  offset += (offsetTarget-offset)*0.05f; 
  tint(255, 153);
  image(b, offset, 20);
}






  static public void main(String args[]) {
    PApplet.main(new String[] { "Transparency" });
  }
}
