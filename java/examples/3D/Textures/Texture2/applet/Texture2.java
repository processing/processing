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

public class Texture2 extends PApplet {

/**
 * Texture 2. 
 * 
 * Using a rectangular image to map a texture onto a triangle.
 */

PImage img;

public void setup() {
  size(640, 360, P3D);
  img = loadImage("berlin-1.jpg");
  noStroke();
}

public void draw() {
  background(0);
  translate(width / 2, height / 2);
  rotateY(map(mouseX, 0, width, -PI, PI));
  beginShape();
  texture(img);
  vertex(-100, -100, 0, 0, 0);
  vertex(100, -40, 0, 400, 120);
  vertex(0, 100, 0, 200, 400);
  endShape();
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Texture2" });
  }
}
