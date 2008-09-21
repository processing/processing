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

public class Recursion2 extends PApplet {

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
  drawCircle(100, 100, 80, 8);
}

public void drawCircle(float x, float y, int radius, int level) 
{                    
  float tt = 126 * level/6.0f;
  fill(tt, 153);
  ellipse(x, y, radius*2, radius*2);      
  if(level > 1) {
    level = level - 1;
    int num = PApplet.parseInt(random(2, 6));
    for(int i=0; i<num; i++) {
      float a = random(0, TWO_PI);
      float nx = x + cos(a) * 6.0f * level;
      float ny = y + sin(a) * 6.0f * level;
      drawCircle(nx, ny, radius/2, level);
    }
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Recursion2" });
  }
}
