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

public class Sine extends PApplet {

/**
 * Sine. 
 * 
 * Smoothly scaling size with the sin() function. 
 */
 
float spin = 0.0f; 
float diameter = 84.0f; 
float angle;

float angle_rot; 
int rad_points = 90;

public void setup() 
{
  size(200, 200);
  noStroke();
  smooth();
}

public void draw() 
{ 
  background(153);
  
  translate(130, 65);
  
  fill(255);
  ellipse(0, 0, 16, 16);
  
  angle_rot = 0;
  fill(51);

  for(int i=0; i<5; i++) {
    pushMatrix();
    rotate(angle_rot + -45);
    ellipse(-116, 0, diameter, diameter);
    popMatrix();
    angle_rot += PI*2/5;
  }

  diameter = 34 * sin(angle) + 168;
  
  angle += 0.02f;
  if (angle > TWO_PI) { angle = 0; }
}


  static public void main(String args[]) {
    PApplet.main(new String[] { "Sine" });
  }
}
