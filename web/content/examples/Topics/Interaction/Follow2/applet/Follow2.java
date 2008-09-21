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

public class Follow2 extends PApplet {

/**
 * Follow 2. 
 * Based on code from Keith Peters (www.bit-101.com). 
 * 
 * A two-segmented arm follows the cursor position. The relative
 * angle between the segments is calculated with atan2() and the
 * position calculated with sin() and cos().
 */

float[] x = new float[2];
float[] y = new float[2];
float segLength = 50;

public void setup() {
  size(200, 200);
  smooth(); 
  strokeWeight(20.0f);
  stroke(0, 100);
}

public void draw() {
  background(226);
  dragSegment(0, mouseX, mouseY);
  dragSegment(1, x[0], y[0]);
}

public void dragSegment(int i, float xin, float yin) {
  float dx = xin - x[i];
  float dy = yin - y[i];
  float angle = atan2(dy, dx);  
  x[i] = xin - cos(angle) * segLength;
  y[i] = yin - sin(angle) * segLength;
  segment(x[i], y[i], angle);
}

public void segment(float x, float y, float a) {
  pushMatrix();
  translate(x, y);
  rotate(a);
  line(0, 0, segLength, 0);
  popMatrix();
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Follow2" });
  }
}
