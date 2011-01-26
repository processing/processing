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

public class Translate extends PApplet {

/**
 * Translate. 
 * 
 * The translate() function allows objects to be moved
 * to any location within the window. The first parameter
 * sets the x-axis offset and the second parameter sets the
 * y-axis offset. 
 */
 
float x, y;
float size = 40.0f;

public void setup() 
{
  size(200,200);
  noStroke();
  frameRate(30);
}

public void draw() 
{
  background(102);
  
  x = x + 0.8f;
  
  if (x > width + size) {
    x = -size;
  } 
  
  translate(x, height/2-size/2);
  fill(255);
  rect(-size/2, -size/2, size, size);
  
  // Transforms accumulate.
  // Notice how this rect moves twice
  // as fast as the other, but it has
  // the same parameter for the x-axis value
  translate(x, size);
  fill(0);
  rect(-size/2, -size/2, size, size);
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Translate" });
  }
}
