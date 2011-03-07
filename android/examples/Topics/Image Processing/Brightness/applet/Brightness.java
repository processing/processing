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

public class Brightness extends PApplet {

/**
 * Brightness
 * by Daniel Shiffman. 
 * 
 * Adjusts the brightness of part of the image
 * Pixels closer to the mouse will appear brighter. 
 */
 
PImage img;

public void setup() {
  size(200, 200);
  frameRate(30);
  img = loadImage("wires.jpg");
}

public void draw() {
  loadPixels();
  for (int x = 0; x < img.width; x++) {
    for (int y = 0; y < img.height; y++ ) {
      // Calculate the 1D location from a 2D grid
      int loc = x + y*img.width;
      // Get the R,G,B values from image
      float r,g,b;
      r = red (img.pixels[loc]);
      //g = green (img.pixels[loc]);
      //b = blue (img.pixels[loc]);
      // Calculate an amount to change brightness based on proximity to the mouse
      float maxdist = 50;//dist(0,0,width,height);
      float d = dist(x,y,mouseX,mouseY);
      float adjustbrightness = 255*(maxdist-d)/maxdist;
      r += adjustbrightness;
      //g += adjustbrightness;
      //b += adjustbrightness;
      // Constrain RGB to make sure they are within 0-255 color range
      r = constrain(r,0,255);
      //g = constrain(g,0,255);
      //b = constrain(b,0,255);
      // Make a new color and set pixel in the window
      //color c = color(r,g,b);
      int c = color(r);
      pixels[loc] = c;
    }
  }
  updatePixels();
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Brightness" });
  }
}
