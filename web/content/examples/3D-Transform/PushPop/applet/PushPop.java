import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class PushPop extends PApplet {// Push Pop
// by REAS <http://reas.com>

// The push() and pop() functions allow for more control over transformations.
// The push function saves the current coordinate system to the stack 
// and pop() restores the prior coordinate system. 

// Created 26 October 2002

float a;                          // Angle of rotation
float offset = PI/24;             // Angle offset between boxes
int num = 12;                     // Number of boxes
int[] colors = new int[num];  // Colors of each box
int safecolor;

boolean pink = true;

public void setup() 
{ 
  size(200, 200, P3D);
  noStroke();  
  framerate(30);
  safecolor = color(153, 153, 153);
  for(int i=0; i<num; i++) {
    colors[i] = color(255 * (i+1)/num, 153 * (num-i)/num, 204);
  }
  lights();
} 
 

public void draw() 
{     
  background(0, 0, 26);
      
  translate(width/2, height/2, -20);
  a+=0.04f;
  if(a >= TWO_PI*2) { 
    a = 0.0f; 
  }    
  for(int i=0; i<num; i++) {
    pushMatrix();
    fill(colors[i]);
    rotateY(a+offset*i);
    rotateX(a/2+offset*i);
    box(width/2);
    popMatrix();
  }
} 
}