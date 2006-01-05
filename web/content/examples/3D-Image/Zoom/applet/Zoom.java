import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Zoom extends PApplet {// Zoom
// by REAS <http://reas.com>

// Move the cursor over the image to alter its position. Click and press
// the mouse to zoom and set the density of the matrix by typing numbers 1-5.
// This program displays a series of lines with their heights corresponding to 
// a color value read from an image. 

// Created 10 August 2002

PImage a;
boolean onetime = true;
int[][] aPixels;
float sval = 1.0f;
float nmx, nmy;
int res = 5;

public void setup() 
{
  size(200, 200, P3D);
  noFill();
  stroke(255);
  aPixels = new int[width][height];
  a = loadImage("ystone08.jpg");
  for(int i=0; i<height; i++) {
    for(int j=0; j<width; j++) {
      aPixels[j][i] = a.pixels[i*width+j];
    }
  }
}

public void draw() 
{
  background(51);
  
  if (abs(mouseX - nmx) > 1.0f) { 
    nmx = nmx + (mouseX-nmx)/20; 
  }
  if (abs(mouseY - nmy) > 1.0f) { 
    nmy += (mouseY-nmy)/20; 
  }
  
  if(mousePressed) { 
    sval += 0.005f; 
  } else {
    sval -= 0.01f; 
  }
  if(sval > 2.5f) { sval = 2.5f; }
  if(sval < 1.0f) { sval = 1.0f; }
  
  translate(width/2+nmx*sval-100, height/2+nmy*sval-100, -50);
  scale(sval);
  rotateZ(PI/9-sval+1.0f);
  rotateX(PI/sval/8-0.125f);
  rotateY(sval/8-0.125f);
  
  translate(-width/2, -height/2, 0);
  
  float rr, gg, bb, tt;
  for(int i=0; i<height; i+=res) {
    for(int j=0; j<width; j+=res) {
      rr = red(aPixels[j][i]); 
      gg = green(aPixels[j][i]);
      bb = blue(aPixels[j][i]);
      tt = rr+gg+bb;
      stroke(rr, gg, gg);
      line(i, j, tt/10-20, i, j, tt/10 );
    }
  }
}

public void keyPressed() {
  if(key == '1') {
    res = 1;
  } else if (key == '2') {
    res = 2; 
  } else if (key == '3') {
    res = 3;
  } else if (key == '4') {
    res = 4;
  } else if (key == '5') {
    res = 5;
  }
}






}