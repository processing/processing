import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Pointillism extends PApplet {// Pointillism
// Daniel Shiffman <http://www.shiffman.net>

// Mouse horizontal location controls size of dots
// Creates a simple pointillist-like effect using ellipse colored
// according to pixels in the image

// Created 2 May 2005

PImage a;
public void setup()
{
  a = loadImage("eames.jpg");
  size(200,200);
  noStroke();
  background(255);
  smooth();
  framerate(30);
}

public void draw()
{ 
  float pointillize = 2.0f + (mouseX / (float) width) * 16.0f; 
  int x = PApplet.toInt(random(a.width));
  int y = PApplet.toInt(random(a.height));
  int loc = x + y*a.width;
  float r = red(a.pixels[loc]);
  float g = green(a.pixels[loc]);
  float b = blue(a.pixels[loc]);
  fill(r,g,b,126);
  ellipse(x,y,pointillize,pointillize);
}
}