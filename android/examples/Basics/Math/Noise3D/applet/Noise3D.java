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

public class Noise3D extends PApplet {

/**
 * Noise3D. 
 * 
 * Using 3D noise to create simple animated texture. 
 * Here, the third dimension ('z') is treated as time. 
 */
 
float increment = 0.01f;
// The noise function's 3rd argument, a global variable that increments once per cycle
float zoff = 0.0f;  
// We will increment zoff differently than xoff and yoff
float zincrement = 0.02f; 

public void setup() {
  size(200,200);
  frameRate(30);
}

public void draw() {
  background(0);
  
  // Optional: adjust noise detail here
  // noiseDetail(8,0.65f);
  
  loadPixels();

  float xoff = 0.0f; // Start xoff at 0
  
  // For every x,y coordinate in a 2D space, calculate a noise value and produce a brightness value
  for (int x = 0; x < width; x++) {
    xoff += increment;   // Increment xoff 
    float yoff = 0.0f;   // For every xoff, start yoff at 0
    for (int y = 0; y < height; y++) {
      yoff += increment; // Increment yoff
      
      // Calculate noise and scale by 255
      float bright = noise(xoff,yoff,zoff)*255;

      // Try using this line instead
      //float bright = random(0,255);
      
      // Set each pixel onscreen to a grayscale value
      pixels[x+y*width] = color(bright,bright,bright);
    }
  }
  updatePixels();
  
  zoff += zincrement; // Increment zoff
  
  
}




  static public void main(String args[]) {
    PApplet.main(new String[] { "Noise3D" });
  }
}
