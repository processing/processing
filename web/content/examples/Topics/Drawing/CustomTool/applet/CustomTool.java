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

public class CustomTool extends PApplet {

/**
 * Custom Tool. 
 * 
 * Move the cursor across the screen to draw. 
 * In addition to creating software tools to simulate pens and pencils, 
 * it is possible to create unique tools to draw with. 
 */

int dots = 1000;
float[] dX = new float[dots];
float[] dY = new float[dots];

float l_0 = 0.0f;
float h_0 = 0.0f;

float legX = 0.0f;
float legY = 0.0f;
float thighX = 0.0f;
float thighY = 0.0f;

float l = 60.0f; // Length of the 'leg'
float h = 90.0f; // Height of the 'leg'

float nmx, nmy = 0.0f;
float mx, my = 0.0f;

int currentValue = 0;
int valdir = 1;

public void setup() 
{
  size(640, 360);
  noStroke();
  smooth();
  background(102);
}

public void draw() 
{
  // Smooth the mouse
  nmx = mouseX;
  nmy = mouseY;
  if((abs(mx - nmx) > 1.0f) || (abs(my - nmy) > 1.0f)) { 
    mx = mx - (mx-nmx)/20.0f;
    my = my - (my-nmy)/20.0f;

    // Set the drawing value
    currentValue += 1* valdir;
    if(currentValue > 255 || currentValue <= 0) {
      valdir *= -1;
    }
  }

  iKinematics();
  kinematics();

  pushMatrix();
  translate(width/2, height/2);
  stroke(currentValue); 
  line(thighX, thighY, legX, legY);
  popMatrix();

  stroke(255);
  point(legX + width/2, legY + height/2);
}

public void kinematics() 
{
  thighX = h*cos(h_0);
  thighY = h*sin(h_0);
  legX = thighX + l*cos(h_0 - l_0);
  legY = thighY + l*sin(h_0 - l_0);
}

public void iKinematics()
{
  float tx = mx - width/2.0f;
  float ty = my - height/2.0f;
  float c2 = (tx*tx + ty*ty - h*h - l*l)/(2*h*l); //in degrees
  float s2 = sqrt(abs(1 - c2*c2)); // the sign here determines the bend in the joint  
  l_0 = -atan2(s2, c2);
  h_0 = atan2(ty, tx) - atan2(l*s2, h+l*c2);
}


  static public void main(String args[]) {
    PApplet.main(new String[] { "CustomTool" });
  }
}
