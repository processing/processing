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

public class Lights2 extends PApplet {

/**
 * Lights 2 
 * by Simon Greenwold. 
 * 
 * Display a box with three different kinds of lights. 
 */

public void setup() 
{
  size(640, 360, P3D);
  noStroke();
}

public void draw() 
{
  background(0);
  translate(width / 2, height / 2);
  
  // Orange point light on the right
  pointLight(150, 100, 0, // Color
             200, -150, 0); // Position

  // Blue directional light from the left
  directionalLight(0, 102, 255, // Color
                   1, 0, 0); // The x-, y-, z-axis direction

  // Yellow spotlight from the front
  spotLight(255, 255, 109, // Color
            0, 40, 200, // Position
            0, -0.5f, -0.5f, // Direction
            PI / 2, 2); // Angle, concentration
  
  rotateY(map(mouseX, 0, width, 0, PI));
  rotateX(map(mouseY, 0, height, 0, PI));
  box(150);
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Lights2" });
  }
}
