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

public class Alphamask extends PApplet {

/**
 * Alpha Mask. 
 * 
 * Loads a "mask" for an image to specify the transparency 
 * in different parts of the image. The two images are blended
 * together using the mask() method of PImage. 
 */
 
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

  static public void main(String args[]) {
    PApplet.main(new String[] { "Alphamask" });
  }
}
