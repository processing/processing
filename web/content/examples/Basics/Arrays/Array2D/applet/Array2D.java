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

public class Array2D extends PApplet {
  public void setup() {/**
 * Array 2D. 
 * 
 * Demonstrates the syntax for creating a two-dimensional (2D) array.
 * Values in a 2D array are accessed through two index values.  
 * 2D arrays are useful for storing images. In this example, each dot 
 * is colored in relation to its distance from the center of the image. 
 */
 
float[][] distances;
float maxDistance;

size(200, 200);
background(0);
maxDistance = dist(width/2, height/2, width, height);
distances = new float[width][height];
for(int i=0; i<height; i++) {
  for(int j=0; j<width; j++) {
    float dist = dist(width/2, height/2, j, i);
    distances[j][i] = dist/maxDistance * 255; 
  }
}

for(int i=0; i<height; i+=2) {
  for(int j=0; j<width; j+=2) {
    stroke(distances[j][i]);
    point(j, i);
  }
}




  noLoop();
} 
  static public void main(String args[]) {
    PApplet.main(new String[] { "Array2D" });
  }
}
