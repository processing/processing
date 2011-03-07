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

public class Noise1D extends PApplet {

/**
 * Noise1D. 
 * 
 * Using 1D Perlin Noise to assign location. 
 */
 
float xoff = 0.0f;
float xincrement = 0.01f; 

public void setup() {
  size(200,200);
  background(0);
  frameRate(30);
  smooth();
  noStroke();
}

public void draw()
{
  // Create an alpha blended background
  fill(0, 10);
  rect(0,0,width,height);
  
  //float n = random(0,width);  // Try this line instead of noise
  
  // Get a noise value based on xoff and scale it according to the window's width
  float n = noise(xoff)*width;
  
  // With each cycle, increment xoff
  xoff += xincrement;
  
  // Draw the ellipse at the value produced by perlin noise
  fill(200);
  ellipse(n,height/2,16,16);
}



  static public void main(String args[]) {
    PApplet.main(new String[] { "Noise1D" });
  }
}
