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

public class Bird extends PApplet {

/**
 * Simple 3D Bird 
 * by Ira Greenberg.  
 * 
 * Using a box and 2 rects to simulate a flying bird. 
 * Trig functions handle the flapping and sinuous movement.
 */

float ang = 0, ang2 = 0, ang3 = 0, ang4 = 0;
float px = 0, py = 0, pz = 0;
float flapSpeed = 0.2f;

public void setup(){
  size(640, 360, P3D);
  noStroke();
}

public void draw(){
  background(0);
  lights();

  // Flight
  px = sin(radians(ang3)) * 170;
  py = cos(radians(ang3)) * 300;
  pz = sin(radians(ang4)) * 500;
  translate(width/2 + px, height/2 + py, -700+pz);
  rotateX(sin(radians(ang2)) * 120);
  rotateY(sin(radians(ang2)) * 50);
  rotateZ(sin(radians(ang2)) * 65);
  
  // Body
  fill(153);
  box(20, 100, 20);

  
  // Left wing
  fill(204);
  pushMatrix();
  rotateY(sin(radians(ang)) * -20);
  rect(-75, -50, 75, 100);
  popMatrix();

  // Right wing
  pushMatrix();
  rotateY(sin(radians(ang)) * 20);
  rect(0, -50, 75, 100);
  popMatrix();

  // Wing flap
  ang += flapSpeed;
  if (ang > 3) {
    flapSpeed *= -1;
  } 
  if (ang < -3) {
    flapSpeed *= -1;
  }

  // Increment angles
  ang2 += 0.01f;
  ang3 += 2.0f;
  ang4 += 0.75f;
}


  static public void main(String args[]) {
    PApplet.main(new String[] { "Bird" });
  }
}
