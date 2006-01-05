import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Alphamask extends PApplet {// Alpha Mask
// by toxi <http://www.toxi.co.uk>

// Loads a "mask" for an image which specifies its transparency 
// in different parts of the image. The two images are blended
// together using the mask() method of PImage.

// Created 29 April 2003
// Updated 7 July 2004

PImage img;
PImage maskImg;

public void setup() 
{
  size(200,200);
  img = loadImage("test.jpg");
  maskImg = loadImage("mask.jpg");
  img.mask(maskImg);
}

public void draw() 
{
  background((mouseX+mouseY)/1.5f);
  image(img, 50, 50);
  image(img, mouseX-50, mouseY-50);
}
}