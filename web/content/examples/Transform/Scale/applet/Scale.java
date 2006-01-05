import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Scale extends PApplet {// Scale
// by Denis Grutze

// Paramenters for the scale() function are values specified 
// as decimal percentages. For example, the method call scale(2.0) 
// will increase the dimension of the shape by 200 percent. 
// Objects always scale from the origin.

// Created 12 January 2003

float a = 0.0f;
float s = 0.0f;

public void setup()
{
  size(200,200);
  noStroke();
  rectMode(CENTER);
  framerate(30);
}

public void draw()
{
  background(102);
  
  a = a + 0.04f;
  s = cos(a)*2;
  
  translate(width/2, height/2);
  scale(s); 
  fill(51);
  rect(0, 0, 50, 50); 
  
  translate(75, 0);
  fill(255);
  scale(s);
  rect(0, 0, 50, 50);       
}
}