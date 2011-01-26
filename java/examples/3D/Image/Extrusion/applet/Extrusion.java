import processing.core.*; 
import processing.xml.*; 

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

public class Extrusion extends PApplet {

/**
 * Extrusion. 
 * 
 * Converts a flat image into spatial data points and rotates the points
 * around the center.
 */

PImage extrude;
int[][] values;
float angle = 0;

public void setup() {
  size(640, 360, P3D);
  
  // Load the image into a new array
  extrude = loadImage("ystone08.jpg");
  extrude.loadPixels();
  values = new int[extrude.width][extrude.height];
  for (int y = 0; y < extrude.height; y++) {
    for (int x = 0; x < extrude.width; x++) {
      int pixel = extrude.get(x, y);
      values[x][y] = PApplet.parseInt(brightness(pixel));
    }
  }
}

public void draw() {
  background(0);
  
  // Update the angle
  angle += 0.005f;
  
  // Rotate around the center axis
  translate(width/2, 0, -128);
  rotateY(angle);  
  translate(-extrude.width/2, 100, -128);
  
  // Display the image mass
  for (int y = 0; y < extrude.height; y++) {
    for (int x = 0; x < extrude.width; x++) {
      stroke(values[x][y]);
      point(x, y, -values[x][y]);
    }
  }

}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Extrusion" });
  }
}
