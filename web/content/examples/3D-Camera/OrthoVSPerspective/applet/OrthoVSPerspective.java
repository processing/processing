import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class OrthoVSPerspective extends PApplet {// Ortho
// by REAS <http://reas.com>

// Click to see the difference between orthographic projection
// and perspective projection as applied to a simple box.

// Created 28 April 2005


public void setup() 
{
  size(200, 200, P3D);
  noStroke();
  fill(204);
}

public void draw() 
{
  background(0);
  lights();
 
  if(mousePressed) {
    float fov = 60.0f; 
    float cameraZ = (height/2.0f) / tan(PI * fov / 360.0f); 
    perspective(fov, PApplet.toFloat(width)/PApplet.toFloat(height), 
                cameraZ/10.0f, cameraZ*10.0f); 
  } else {
    ortho(-width/2, width/2, -height/2, height/2, -10, 10); 
  }
  
  translate(100, 100);
  rotateX(-PI/6); 
  rotateY(PI/3); 
  box(85); 
}

}