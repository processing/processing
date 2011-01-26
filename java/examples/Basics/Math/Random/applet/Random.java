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

public class Random extends PApplet {
  public void setup() {/**
 * Random. 
 * 
 * Random numbers create the basis of this image. 
 * Each time the program is loaded the result is different. 
 */
 
size(200, 200);
smooth();
background(0);
strokeWeight(10);

for(int i = 0; i < width; i++) {
  float r = random(255);
  float x = random(0, width);
  stroke(r, 100);
  line(i, 0, x, height);
}


  noLoop();
} 
  static public void main(String args[]) {
    PApplet.main(new String[] { "Random" });
  }
}
