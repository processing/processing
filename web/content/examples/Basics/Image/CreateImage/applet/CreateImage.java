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

public class CreateImage extends PApplet {

/**
 * Create Image. 
 * 
 * The createImage() function provides a fresh buffer of pixels to play with.
 * This example creates an image gradient.
 */

PImage img;

public void setup() 
{
  size(200, 200);  
  img = createImage(120, 120, ARGB);
  for(int i=0; i < img.pixels.length; i++) {
    img.pixels[i] = color(0, 90, 102, i%img.width * 2); 
  }
}

public void draw() 
{
  background(204);
  image(img, 33, 33);
  image(img, mouseX-60, mouseY-60);
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "CreateImage" });
  }
}
