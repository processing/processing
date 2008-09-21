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

public class Texture3 extends PApplet {

/**
 * Texture 3. 
 * 
 * Load an image and draw it onto a cylinder and a quad. 
 */


int tubeRes = 32;
float[] tubeX = new float[tubeRes];
float[] tubeY = new float[tubeRes];
PImage img;

public void setup() {
  size(640, 360, P3D);
  img = loadImage("berlin-1.jpg");
  float angle = 270.0f / tubeRes;
  for (int i = 0; i < tubeRes; i++) {
    tubeX[i] = cos(radians(i * angle));
    tubeY[i] = sin(radians(i * angle));
  }
  noStroke();
}

public void draw() {
  background(0);
  translate(width / 2, height / 2);
  rotateX(map(mouseY, 0, height, -PI, PI));
  rotateY(map(mouseX, 0, width, -PI, PI));
  beginShape(QUAD_STRIP);
  texture(img);
  for (int i = 0; i < tubeRes; i++) {
    float x = tubeX[i] * 100;
    float z = tubeY[i] * 100;
    float u = img.width / tubeRes * i;
    vertex(x, -100, z, u, 0);
    vertex(x, 100, z, u, img.height);
  }
  endShape();
  beginShape(QUADS);
  texture(img);
  vertex(0, -100, 0, 0, 0);
  vertex(100, -100, 0, 100, 0);
  vertex(100, 100, 0, 100, 100);
  vertex(0, 100, 0, 0, 100);
  endShape();
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Texture3" });
  }
}
