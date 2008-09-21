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

public class SimpleCurves extends PApplet {

/**
 * Simple Curves. 
 * 
 * Simple curves are drawn with simple equations. 
 * By using numbers with values between 0 and 1 in
 * the equations, a series of elegant curves
 * are created. The numbers are then scaled to fill the screen. 
 */

public void setup() {
  size(200, 200);
  colorMode(RGB, 100);
  background(0);
  noFill();
  noLoop();
}

public void draw() {
  stroke(40);
  beginShape();
  for(int i=0; i<width; i++) {
   vertex(i, singraph((float)i/width)*height);
  }
  endShape();
  
  stroke(55);
  beginShape();
  for(int i=0; i<width; i++) {
   vertex(i, quad((float)i/width)*height);
  }
  endShape();
  
  stroke(70);
  beginShape();
  for(int i=0; i<width; i++) {
   vertex(i, quadHump((float)i/width)*height);
  }
  endShape();
  
  stroke(85);
  beginShape();
  for(int i=0; i<width; i++) {
   vertex(i, hump((float)i/width)*height);
  }
  endShape();
  
  stroke(100);
  beginShape();
  for(int i=0; i<width; i++) {
   vertex(i, squared((float)i/width)*height);
  }
  endShape();
}

public float singraph(float sa) {
  sa = (sa - 0.5f) * 1.0f; //scale from -1 to 1
  sa = sin(sa*PI)/2 + 0.5f;
  return sa;
}

public float quad(float sa) {
  return sa*sa*sa*sa;
}

public float quadHump(float sa) {
  sa = (sa - 0.5f); //scale from -2 to 2
  sa = sa*sa*sa*sa * 16;
  return sa;
}

public float hump(float sa) {
  sa = (sa - 0.5f) * 2; //scale from -2 to 2
  sa = sa*sa;
  if(sa > 1) { sa = 1; }
  return 1-sa;
}

public float squared(float sa) {
  sa = sa*sa;
  return sa;
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "SimpleCurves" });
  }
}
