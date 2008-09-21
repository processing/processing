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

public class Reflection extends PApplet {

/**
 * Reflection 
 * by Simon Greenwold. 
 * 
 * Vary the specular reflection component of a material
 * with the horizontal position of the mouse. 
 */

public void setup() {
  size(640, 360, P3D);
  noStroke();
  colorMode(RGB, 1);
  fill(0.4f);
}

public void draw() {
  background(0);
  translate(width / 2, height / 2);
  // Set the specular color of lights that follow
  lightSpecular(1, 1, 1);
  directionalLight(0.8f, 0.8f, 0.8f, 0, 0, -1);
  float s = mouseX / PApplet.parseFloat(width);
  specular(s, s, s);
  sphere(120);
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Reflection" });
  }
}
