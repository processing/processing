import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Rotate3D extends PApplet {// Rotate 3D
// by REAS <http://reas.com>

// Rotating simultaneously in the X and Y axis. 
// Transformation functions such as rotate() are additive.
// Successively calling rotate(1.0) and rotate(2.0)
// is equivalent to calling rotate(3.0).

// Updated 21 August 2002

float a = 0.0f;
float rSize;  // rectangle size

public void setup() 
{
  size(200, 200, P3D);
  rSize = width/4;  
  noStroke();
  fill(204, 204);
  framerate(30);
}

public void draw() 
{
  background(0);
  
  a += 0.01f;
  if(a > TWO_PI) { 
    a = 0.0f; 
  }
  
  translate(width/2, height/2);
  
  rotateX(a);
  rotateY(a*2);
  rect(-rSize, -rSize, rSize*2, rSize*2);
  
  rotateX(a*1.001f);
  rotateY(a*2.002f);
  rect(-rSize, -rSize, rSize*2, rSize*2);

}
}