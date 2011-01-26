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

public class Arm extends PApplet {

/**
 * Arm. 
 * 
 * The angle of each segment is controlled with the mouseX and
 * mouseY position. The transformations applied to the first segment
 * are also applied to the second segment because they are inside
 * the same pushMatrix() and popMatrix() group.
*/

float x = 50;
float y = 100;
float angle1 = 0.0f;
float angle2 = 0.0f;
float segLength = 50;

public void setup() {
  size(200, 200);
  smooth(); 
  strokeWeight(20.0f);
  stroke(0, 100);
}

public void draw() {
  background(226);
  
  angle1 = (mouseX/PApplet.parseFloat(width) - 0.5f) * -PI;
  angle2 = (mouseY/PApplet.parseFloat(height) - 0.5f) * PI;
  
  pushMatrix();
  segment(x, y, angle1); 
  segment(segLength, 0, angle2);
  popMatrix();
}

public void segment(float x, float y, float a) {
  translate(x, y);
  rotate(a);
  line(0, 0, segLength, 0);
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Arm" });
  }
}
