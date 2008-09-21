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

public class MoveEye extends PApplet {

/**
 * Move Eye. 
 * by Simon Greenwold.
 * 
 * The camera lifts up (controlled by mouseY) while looking at the same point.
 */

public void setup() {
  size(640, 360, P3D);
  fill(204);
}

public void draw() {
  lights();
  background(0);
  
  // Change height of the camera with mouseY
  camera(30.0f, mouseY, 220.0f, // eyeX, eyeY, eyeZ
         0.0f, 0.0f, 0.0f, // centerX, centerY, centerZ
         0.0f, 1.0f, 0.0f); // upX, upY, upZ
  
  noStroke();
  box(90);
  stroke(255);
  line(-100, 0, 0, 100, 0, 0);
  line(0, -100, 0, 0, 100, 0);
  line(0, 0, -100, 0, 0, 100);
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "MoveEye" });
  }
}
