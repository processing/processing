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

public class Distance2D extends PApplet {

/**
 * Distance 2D. 
 * 
 * Move the mouse across the image to obscure and reveal the matrix.  
 * Measures the distance from the mouse to each square and sets the
 * size proportionally. 
 */
 
float max_distance;

public void setup() {
  size(200, 200); 
  smooth();
  noStroke();
  max_distance = dist(0, 0, width, height);
}

public void draw() 
{
  background(51);

  for(int i = 0; i <= width; i += 20) {
    for(int j = 0; j <= height; j += 20) {
      float size = dist(mouseX, mouseY, i, j);
      size = size/max_distance * 66;
      ellipse(i, j, size, size);
    }
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Distance2D" });
  }
}
