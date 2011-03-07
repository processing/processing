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

public class Recursion extends PApplet {

/**
 * Recursion. 
 * 
 * A demonstration of recursion, which means functions call themselves. 
 * Notice how the drawCircle() function calls itself at the end of its block. 
 * It continues to do this until the variable "level" is equal to 1. 
 */
 
public void setup() 
{
  size(200, 200);
  noStroke();
  smooth();
  noLoop();
}

public void draw() 
{
  drawCircle(126, 170, 6);
}

public void drawCircle(int x, int radius, int level) 
{                    
  float tt = 126 * level/4.0f;
  fill(tt);
  ellipse(x, 100, radius*2, radius*2);      
  if(level > 1) {
    level = level - 1;
    drawCircle(x - radius/2, radius/2, level);
    drawCircle(x + radius/2, radius/2, level);
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Recursion" });
  }
}
