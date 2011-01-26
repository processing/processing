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

public class Texture1 extends PApplet {

/**
 * Texture 1. 
 * 
 * Load an image and draw it onto a quad. The texture() function sets
 * the texture image. The vertex() function maps the image to the geometry.
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
  rotateZ(PI/6);
  beginShape();
  texture(img);
  vertex(-100, -100, 0, 0, 0);
  vertex(100, -100, 0, 400, 0);
  vertex(100, 100, 0, 400, 400);
  vertex(-100, 100, 0, 0, 400);
  endShape();
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Texture1" });
  }
}
