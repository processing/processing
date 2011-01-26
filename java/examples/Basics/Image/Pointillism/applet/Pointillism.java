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

public class Pointillism extends PApplet {

/**
 * Pointillism
 * by Daniel Shiffman. 
 * 
 * Mouse horizontal location controls size of dots. 
 * Creates a simple pointillist effect using ellipses colored
 * according to pixels in an image. 
 */
 
PImage a;

public void setup()
{
  a = loadImage("eames.jpg");
  size(200,200);
  noStroke();
  background(255);
  smooth();
}

public void draw()
{ 
  float pointillize = map(mouseX, 0, width, 2, 18);
  int x = PApplet.parseInt(random(a.width));
  int y = PApplet.parseInt(random(a.height));
  int pix = a.get(x, y);
  fill(pix, 126);
  ellipse(x, y, pointillize, pointillize);
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Pointillism" });
  }
}
