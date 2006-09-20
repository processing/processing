import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Functions extends PApplet {/**
 * Functions. 
 * 
 * The draw_target() function makes it easy to draw many distinct targets. 
 * Each call to draw_target() specifies the position, size, and number of 
 * rings for each target. 
 * 
 * Updated 21 August 2002
 */

public void setup() 
{
  size(200, 200);
  background(51);
  noStroke();
  smooth();
  noLoop();
}

public void draw() 
{
  draw_target(68, 34, 200, 10);
  draw_target(152, 16, 100, 3);
  draw_target(100, 144, 80, 5);
}

public void draw_target(int xloc, int yloc, int size, int num) 
{
  float grayvalues = 255/num;
  float steps = size/num;
  for(int i=0; i<num; i++) {
    fill(i*grayvalues);
    ellipse(xloc, yloc, size-i*steps, size-i*steps);
  }
}
static public void main(String args[]) {   PApplet.main(new String[] { "Functions" });}}