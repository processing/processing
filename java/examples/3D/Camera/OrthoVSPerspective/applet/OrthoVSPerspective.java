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

public class OrthoVSPerspective extends PApplet {

/** 
 * Ortho vs Perspective. 
 * 
 * Click to see the difference between orthographic projection
 * and perspective projection as applied to a simple box.
 * The ortho() function sets an orthographic projection and 
 * defines a parallel clipping volume. All objects with the 
 * same dimension appear the same size, regardless of whether 
 * they are near or far from the camera. The parameters to this 
 * function specify the clipping volume where left and right 
 * are the minimum and maximum x values, top and bottom are the 
 * minimum and maximum y values, and near and far are the minimum 
 * and maximum z values.
 */

public void setup() 
{
  size(640, 360, P3D);
  noStroke();
  fill(204);
}

public void draw() 
{
  background(0);
  lights();
 
  if(mousePressed) {
    float fov = PI/3.0f; 
    float cameraZ = (height/2.0f) / tan(PI * fov / 360.0f); 
    perspective(fov, PApplet.parseFloat(width)/PApplet.parseFloat(height), 
                cameraZ/2.0f, cameraZ*2.0f); 
  } else {
    ortho(-width/2, width/2, -height/2, height/2, -10, 10); 
  }
  
  translate(width/2, height/2, 0);
  rotateX(-PI/6); 
  rotateY(PI/3); 
  box(160); 
}


  static public void main(String args[]) {
    PApplet.main(new String[] { "OrthoVSPerspective" });
  }
}
