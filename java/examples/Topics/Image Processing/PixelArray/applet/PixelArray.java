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

public class PixelArray extends PApplet {

/**
 * Pixel Array. 
 * 
 * Click and drag the mouse up and down to control the signal and 
 * press and hold any key to see the current pixel being read. 
 * This program sequentially reads the color of every pixel of an image
 * and displays this color to fill the window.  
 */
 
PImage a;
int[] aPixels;
int direction = 1;
boolean onetime = true;
float signal;

public void setup() 
{
  size(200, 200);
  aPixels = new int[width*height];
  noFill();
  stroke(255);
  frameRate(30);
  a = loadImage("ystone08.jpg");
  for(int i=0; i<width*height; i++) {
    aPixels[i] = a.pixels[i];
  }
}

public void draw() 
{
  if (signal > width*height-1 || signal < 0) { 
    direction = direction * -1; 
  }

  if(mousePressed) {
    if(mouseY > height-1) { mouseY = height-1; }
    if(mouseY < 0) { mouseY = 0; }
    signal = mouseY*width+mouseX;
  } else {
    signal += (0.33f*direction);  
  }
  
  if(keyPressed) {
    loadPixels();
    for (int i=0; i<width*height; i++) { 
      pixels[i] = aPixels[i];  
    }
    updatePixels();
    rect(signal%width-5, PApplet.parseInt(signal/width)-5, 10, 10);
    point(signal%width, PApplet.parseInt(signal/width));
  } else {
    loadPixels();
    for (int i=0; i<width*height; i++) { 
      pixels[i] = aPixels[PApplet.parseInt(signal)];
    }
    updatePixels();
  }
}






  static public void main(String args[]) {
    PApplet.main(new String[] { "PixelArray" });
  }
}
