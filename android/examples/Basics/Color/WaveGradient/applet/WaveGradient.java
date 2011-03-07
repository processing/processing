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

public class WaveGradient extends PApplet {

/**
 * Wave Gradient 
 * by Ira Greenberg.  
 * 
 * Generate a gradient along a sin() wave.
 */

float angle = 0;
float px = 0, py = 0;
float amplitude = 30;
float frequency = 0;
float fillGap = 2.5f;
int c;

public void setup() {
  size(200, 200);
  background(200,200,200);
  noLoop();
}

public void draw() {
  for (int i =- 75; i < height+75; i++){
    // Reset angle to 0, so waves stack properly
    angle = 0;
    // Increasing frequency causes more gaps
    frequency+=.006f;
    for (float j=0; j<width+75; j++){
      py = i+sin(radians(angle))*amplitude;
      angle+=frequency;
      c =  color(abs(py-i)*255/amplitude, 255-abs(py-i)*255/amplitude, j*(255.0f/(width+50)));
      // Hack to fill gaps. Raise value of fillGap if you increase frequency
      for (int filler = 0; filler<fillGap; filler++){
        set(PApplet.parseInt(j-filler), PApplet.parseInt(py)-filler, c);
        set(PApplet.parseInt(j), PApplet.parseInt(py), c);
        set(PApplet.parseInt(j+filler), PApplet.parseInt(py)+filler, c);
      }
    }
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "WaveGradient" });
  }
}
