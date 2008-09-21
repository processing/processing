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

public class LinearImage extends PApplet {

/**
 * Linear Image. 
 * 
 * Click and drag mouse up and down to control the signal. 
 * Press and hold any key to watch the scanning. 
 */
 
PImage a;
boolean onetime = true;
int[] aPixels = new int[200*200];
int direction = 1;

float signal;

public void setup() 
{
  size(200, 200);
  stroke(255);
  a = loadImage("florence03.jpg");
  for(int i=0; i<width*height; i++) {
    aPixels[i] = a.pixels[i];
  }
}

public void draw() 
{
  if (signal > width-1 || signal < 0) { 
    direction = direction * -1; 
  }

  if(mousePressed) {
    signal = abs(mouseY%height);
  } else {
    signal += (0.3f*direction);  
  }
  
  
  if(keyPressed) {
    loadPixels();
    for (int i=0; i<width*height; i++) { 
      pixels[i] = aPixels[i];  
    }
    updatePixels();
    line(0, signal, width, signal);
  } else {
    loadPixels();
    for (int i=0; i<width*height; i++) { 
      pixels[i] = aPixels[PApplet.parseInt((width*PApplet.parseInt(signal))+(i%width))];
    }
    updatePixels();
  }
  
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "LinearImage" });
  }
}
