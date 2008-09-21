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

public class TriangleStrip extends PApplet {
  public void setup() {/**
 * TRIANGLE_STRIP Mode
 * By Ira Greenberg 
 * 
 * Generate a closed ring using vertex() 
 * function and beginShape(TRIANGLE_STRIP)
 * mode. outerRad and innerRad variables 
 * control ring's outer/inner radii respectively.
 * Trig functions generate ring.
 */

size(200, 200);
background(204);
smooth();

int x = width/2;
int y = height/2;
int outerRad = 80;
int innerRad = 50;
float px = 0, py = 0, angle = 0;
float pts = 36;
float rot = 360.0f/pts;

beginShape(TRIANGLE_STRIP); 
for (int i = 0; i < pts; i++) {
  px = x + cos(radians(angle))*outerRad;
  py = y + sin(radians(angle))*outerRad;
  angle += rot;
  vertex(px, py);
  px = x + cos(radians(angle))*innerRad;
  py = y + sin(radians(angle))*innerRad;
  vertex(px, py); 
  angle += rot;
}
endShape();


  noLoop();
} 
  static public void main(String args[]) {
    PApplet.main(new String[] { "TriangleStrip" });
  }
}
