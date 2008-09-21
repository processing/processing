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

public class Rotate2 extends PApplet {

/**
 * Rotate 2. 
 * 
 * The push() and pop() functions allow for more control over transformations.
 * The push function saves the current coordinate system to the stack 
 * and pop() restores the prior coordinate system. 
 */
 
float a;                          // Angle of rotation
float offset = PI/24.0f;             // Angle offset between boxes
int num = 12;                     // Number of boxes
int[] colors = new int[num];  // Colors of each box
int safecolor;

boolean pink = true;

public void setup() 
{ 
  size(640, 360, P3D);
  noStroke();  
  for(int i=0; i<num; i++) {
    colors[i] = color(255 * (i+1)/num);
  }
  lights();
} 
 

public void draw() 
{     
  background(0, 0, 26);
  translate(width/2, height/2);
  a += 0.01f;   
  
  for(int i = 0; i < num; i++) {
    pushMatrix();
    fill(colors[i]);
    rotateY(a + offset*i);
    rotateX(a/2 + offset*i);
    box(200);
    popMatrix();
  }
} 

  static public void main(String args[]) {
    PApplet.main(new String[] { "Rotate2" });
  }
}
