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

public class DoubleRandom extends PApplet {
  public void setup() {/**
 * Double Random 
 * by Ira Greenberg.  
 * 
 * Using 2 random() calls the and point() function 
 * to create an irregular sawtooth line.
 */

size(200, 200);
background(0);
int totalPts = 300;
float steps = totalPts+1;
stroke(255);
float rand = 0;

for  (int i=1; i< steps; i++){
  point( (width/steps) * i, (height/2) + random(-rand, rand) );
  rand += random(-5, 5);
}


  noLoop();
} 
  static public void main(String args[]) {
    PApplet.main(new String[] { "DoubleRandom" });
  }
}
