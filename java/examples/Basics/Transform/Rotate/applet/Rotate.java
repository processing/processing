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

public class Rotate extends PApplet {

/**
 * Rotate. 
 * 
 * Rotating a square around the Z axis. To get the results
 * you expect, send the rotate function angle parameters that are
 * values between 0 and PI*2 (TWO_PI which is roughly 6.28). If you prefer to 
 * think about angles as degrees (0-360), you can use the radians() 
 * method to convert your values. For example: scale(radians(90))
 * is identical to the statement scale(PI/2). 
 */
 
public void setup()
{
  size(200,200);
  noStroke();
  fill(255);
  frameRate(30);
}

float angle;
float cosine;
float jitter;

public void draw()
{
  background(102);
  
  if(second()%2 == 0){
    jitter = (random(-0.1f, 0.1f));
  }
  angle = angle + jitter;
  cosine = cos(angle);
  
  translate(width/2, height/2);
  rotate(cosine);
  rectMode(CENTER);
  rect(0, 0, 115, 115);   
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Rotate" });
  }
}
