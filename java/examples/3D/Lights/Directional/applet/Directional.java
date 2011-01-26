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

public class Directional extends PApplet {

/**
 * Directional. 
 * 
 * Move the mouse the change the direction of the light.
 * Directional light comes from one direction and is stronger 
 * when hitting a surface squarely and weaker if it hits at a 
 * a gentle angle. After hitting a surface, a directional lights 
 * scatters in all directions. 
 */

public void setup() {
  size(640, 360, P3D);
  noStroke();
  fill(204);
}

public void draw() {
  noStroke(); 
  background(0); 
  float dirY = (mouseY / PApplet.parseFloat(height) - 0.5f) * 2;
  float dirX = (mouseX / PApplet.parseFloat(width) - 0.5f) * 2;
  directionalLight(204, 204, 204, -dirX, -dirY, -1); 
  translate(width/2 - 100, height/2, 0); 
  sphere(80); 
  translate(200, 0, 0); 
  sphere(80); 
}


  static public void main(String args[]) {
    PApplet.main(new String[] { "Directional" });
  }
}
