import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class RGBCube extends PApplet {// RGB Cube
// by fry <http://benfry.com>

// The three primary colors of the additive color model are red, green, and blue.
// This RGB color cube displays smooth transitions between these colors. 

// Created 25 October 2002

float xmag, ymag = 0;
float newXmag, newYmag = 0; 
 
public void setup() 
{ 
  size(200, 200, P3D); 
  noStroke(); 
  colorMode(RGB, 1); 
} 
 
public void draw() 
{ 
  background(0.5f, 0.5f, 0.45f);
  
  pushMatrix(); 
 
  translate(width/2, height/2, -30); 
  
  newXmag = mouseX/PApplet.toFloat(width) * TWO_PI;
  newYmag = mouseY/PApplet.toFloat(height) * TWO_PI;
  
  float diff = xmag-newXmag;
  if (abs(diff) >  0.01f) { xmag -= diff/4.0f; }
  
  diff = ymag-newYmag;
  if (abs(diff) >  0.01f) { ymag -= diff/4.0f; }
  
  rotateX(-ymag); 
  rotateY(-xmag); 
  
  scale(50);
  beginShape(QUADS);

  fill(0, 1, 1); vertex(-1,  1,  1);
  fill(1, 1, 1); vertex( 1,  1,  1);
  fill(1, 0, 1); vertex( 1, -1,  1);
  fill(0, 0, 1); vertex(-1, -1,  1);

  fill(1, 1, 1); vertex( 1,  1,  1);
  fill(1, 1, 0); vertex( 1,  1, -1);
  fill(1, 0, 0); vertex( 1, -1, -1);
  fill(1, 0, 1); vertex( 1, -1,  1);

  fill(1, 1, 0); vertex( 1,  1, -1);
  fill(0, 1, 0); vertex(-1,  1, -1);
  fill(0, 0, 0); vertex(-1, -1, -1);
  fill(1, 0, 0); vertex( 1, -1, -1);

  fill(0, 1, 0); vertex(-1,  1, -1);
  fill(0, 1, 1); vertex(-1,  1,  1);
  fill(0, 0, 1); vertex(-1, -1,  1);
  fill(0, 0, 0); vertex(-1, -1, -1);

  fill(0, 1, 0); vertex(-1,  1, -1);
  fill(1, 1, 0); vertex( 1,  1, -1);
  fill(1, 1, 1); vertex( 1,  1,  1);
  fill(0, 1, 1); vertex(-1,  1,  1);

  fill(0, 0, 0); vertex(-1, -1, -1);
  fill(1, 0, 0); vertex( 1, -1, -1);
  fill(1, 0, 1); vertex( 1, -1,  1);
  fill(0, 0, 1); vertex(-1, -1,  1);

  endShape();
  
  popMatrix(); 
} 
}