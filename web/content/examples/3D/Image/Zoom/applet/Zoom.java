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

public class Zoom extends PApplet {

/**
 * Zoom. 
 * 
 * Move the cursor over the image to alter its position. Click and press
 * the mouse to zoom and set the density of the matrix by typing numbers 1-5.
 * This program displays a series of lines with their heights corresponding to 
 * a color value read from an image. 
 */

PImage img;
//boolean onetime = true;
int[][] imgPixels;
float sval = 1.0f;
float nmx, nmy;
int res = 5;

public void setup() 
{
  size(640, 360, P3D);
  noFill();
  stroke(255);
  img = loadImage("ystone08.jpg");
  imgPixels = new int[img.width][img.height];
  for (int i = 0; i < img.height; i++) {
    for (int j = 0; j < img.width; j++) {
      imgPixels[j][i] = img.get(j, i);
    }
  }
}

public void draw() 
{
  background(0);

  nmx = nmx + (mouseX-nmx)/20; 
  nmy += (mouseY-nmy)/20; 

  if(mousePressed) { 
    sval += 0.005f; 
  } 
  else {
    sval -= 0.01f; 
  }

  sval = constrain(sval, 1.0f, 2.5f);

  translate(width/2 + nmx * sval-100, height/2 + nmy*sval - 200, -50);
  scale(sval);
  rotateZ(PI/9 - sval + 1.0f);
  rotateX(PI/sval/8 - 0.125f);
  rotateY(sval/8 - 0.125f);

  translate(-width/2, -height/2, 0);

  for (int i = 0; i < img.height; i += res) {
    for (int j = 0; j < img.width; j += res) {
      float rr = red(imgPixels[j][i]); 
      float gg = green(imgPixels[j][i]);
      float bb = blue(imgPixels[j][i]);
      float tt = rr+gg+bb;
      stroke(rr, gg, gg);
      line(i, j, tt/10-20, i, j, tt/10 );
    }
  }
}

public void keyPressed() {
  if(key == '1') {
    res = 1;
  } 
  else if (key == '2') {
    res = 2; 
  } 
  else if (key == '3') {
    res = 3;
  } 
  else if (key == '4') {
    res = 4;
  } 
  else if (key == '5') {
    res = 5;
  }
}







  static public void main(String args[]) {
    PApplet.main(new String[] { "Zoom" });
  }
}
