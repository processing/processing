import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Sine extends PApplet {// Sine
// by REAS <http://reas.com>

// Smoothly scaling size with the sin() function.

// Updated 21 August 2002


float spin = 0.0f; 
float radius = 42.0f; 
float angle;

float angle_rot; 
int rad_points = 90;

public void setup() 
{
  size(200, 200);
  noStroke();
  ellipseMode(CENTER_RADIUS);
  smooth();
  framerate(30);
}

public void draw() 
{ 
  background(153);
  
  translate(130, 65);
  
  fill(255);
  ellipse(0, 0, 8, 8);
  
  angle_rot = 0;
  fill(51);

  for(int i=0; i<5; i++) {
    pushMatrix();
    rotate(angle_rot + -45);
    ellipse(-116, 0, radius, radius);
    popMatrix();
    angle_rot += PI*2/5;
  }

  radius = 17 * sin(angle) + 84;
  
  angle += 0.03f;
  if (angle > TWO_PI) { angle = 0; }
}

}