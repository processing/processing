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

public class Modulo extends PApplet {

/**
 * Modulo. 
 * 
 * The modulo operator (%) returns the remainder of a number 
 * divided by another. As in this example, it is often used 
 * to keep numerical values within a set range. 
 */
 
int num = 20;
float c;

public void setup()
{
  size(200,200);
  fill(255);
  frameRate(30);
}

public void draw() 
{ 
  background(0);
  c+=0.1f;
  for(int i=1; i<height/num; i++) { 
    float x = (c%i)*i*i;
    stroke(102);
    line(0, i*num, x, i*num);
    noStroke();
    rect(x, i*num-num/2, 8, num);
  } 
} 


  static public void main(String args[]) {
    PApplet.main(new String[] { "Modulo" });
  }
}
