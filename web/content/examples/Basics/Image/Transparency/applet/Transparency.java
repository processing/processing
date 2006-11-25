import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Transparency extends PApplet {// Transparency
// by REAS <http://reas.com>

// Move the pointer left and right across the image to change
// its position. This program overlays one image over another 
// by modifying the alpha value of the image.

// Created 09 December 2002


PImage a, b;
boolean once = false;
int[] buffer;
float bufferOffset, newBufferOffset;

public void setup() 
{
  size(200, 200);
  buffer = new int[width*height];
  bufferOffset = newBufferOffset = 0.0f;
  a = loadImage("construct.jpg");  // Load an image into the program 
  b = loadImage("wash.jpg");   // Load an image into the program 
  framerate(60);
}

public void draw() 
{ 
  image(a, 0, 0);
  
  newBufferOffset = -b.width/2 + (mouseX*2-width/2);
  float distance = bufferOffset - newBufferOffset;
  if( abs(distance) > 0.01f ) {
    bufferOffset -= distance/10.0f;
    bufferOffset = constrain(bufferOffset, -400, 0);
  }
  tint(255, 153);
  image(b, bufferOffset, 20);
}





}