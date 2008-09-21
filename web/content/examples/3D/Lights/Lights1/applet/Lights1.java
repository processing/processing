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

public class Lights1 extends PApplet {

/**
 * Lights 1. 
 * 
 * Uses the default lights to show a simple box. The lights() function
 * is used to turn on the default lighting.
 */
 
float spin = 0.0f;

public void setup() 
{
  size(640, 360, P3D);
  noStroke();
}

public void draw() 
{
  background(51);
  lights();
  
  spin += 0.01f;
  
  pushMatrix();
  translate(width/2, height/2, 0);
  rotateX(PI/9);
  rotateY(PI/5 + spin);
  box(150);
  popMatrix();
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Lights1" });
  }
}
