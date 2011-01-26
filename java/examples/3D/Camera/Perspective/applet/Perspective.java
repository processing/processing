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

public class Perspective extends PApplet {

/**
 * Perspective. 
 * 
 * Move the mouse left and right to change the field of view (fov).
 * Click to modify the aspect ratio. The perspective() function
 * sets a perspective projection applying foreshortening, making 
 * distant objects appear smaller than closer ones. The parameters 
 * define a viewing volume with the shape of truncated pyramid. 
 * Objects near to the front of the volume appear their actual size, 
 * while farther objects appear smaller. This projection simulates 
 * the perspective of the world more accurately than orthographic projection. 
 * The version of perspective without parameters sets the default 
 * perspective and the version with four parameters allows the programmer 
 * to set the area precisely.
 */

public void setup() {
  size(640, 360, P3D);
  noStroke();
}

public void draw() {
  lights();
  background(204);
  float cameraY = height/2.0f;
  float fov = mouseX/PApplet.parseFloat(width) * PI/2;
  float cameraZ = cameraY / tan(fov / 2.0f);
  float aspect = PApplet.parseFloat(width)/PApplet.parseFloat(height);
  if (mousePressed) {
    aspect = aspect / 2.0f;
  }
  perspective(fov, aspect, cameraZ/10.0f, cameraZ*10.0f);
  
  translate(width/2+30, height/2, 0);
  rotateX(-PI/6);
  rotateY(PI/3 + mouseY/PApplet.parseFloat(height) * PI);
  box(45);
  translate(0, 0, -50);
  box(30);
}


  static public void main(String args[]) {
    PApplet.main(new String[] { "Perspective" });
  }
}
