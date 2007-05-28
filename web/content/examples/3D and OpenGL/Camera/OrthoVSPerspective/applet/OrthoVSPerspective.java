import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class OrthoVSPerspective extends PApplet {/** 
 * Ortho vs Perspective. 
 * 
 * Click to see the difference between orthographic projection
 * and perspective projection as applied to a simple box.
 * 
 * Created 28 April 2005
 */

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
    float fov = PI/3.0f; 
    float cameraZ = (height/2.0f) / tan(PI * fov / 360.0f); 
    perspective(fov, PApplet.parseFloat(width)/PApplet.parseFloat(height), 
                cameraZ/2.0f, cameraZ*2.0f); 
  } else {
    ortho(-width/2, width/2, -height/2, height/2, -10, 10); 
  }
  
  translate(100, 100, 0);
  rotateX(-PI/6); 
  rotateY(PI/3); 
  box(85); 
}

static public void main(String args[]) {   PApplet.main(new String[] { "OrthoVSPerspective" });}}