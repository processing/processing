import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class BackgroundImage extends PApplet {// Background image
// by REAS <http://reas.com>

// This example presents the fastest way to load a background image
// into Processing.

// Created 16 January 2003

PImage bg;
int a; 

public void setup() 
{
  size(200,200);
  framerate(30);
  // The background image must be the same size as the parameters
  // into the size() method. In this program, the size of "milan_rubbish.jpg"
  // is 200 x 200 pixels.
  bg = loadImage("milan_rubbish.jpg");
}

public void draw() 
{
  background(bg);

  a = (a + 1)%(width+32);
  stroke(226, 204, 0);
  line(0, a, width, a-26);
  line(0, a-6, width, a-32);
}
}