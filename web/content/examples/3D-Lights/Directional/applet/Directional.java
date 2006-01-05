import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Directional extends PApplet {// Directional
// by REAS <http://reas.com>

// Move the mouse the change the direction of the light.
// Directional light comes from one direction and is stronger 
// when hitting a surface squarely and weaker if it hits at a 
// a gentle angle. After hitting a surface, a directional lights 
// scatters in all directions.

// Created 28 April 2005


public void setup() 
{
  size(200, 200, P3D);
  noStroke();
  fill(204);
}

public void draw() 
{
  noStroke(); 
  background(0); 
  float dirY = (mouseY/PApplet.toFloat(height) - 0.5f) * 2.0f;
  float dirX = (mouseX/PApplet.toFloat(width) - 0.5f) * 2.0f;
  directionalLight(204, 204, 204, -dirX, -dirY, -1); 
  translate(20, height/2, 0); 
  sphere(60); 
  translate(120, 0, 0); 
  sphere(60); 
}

}