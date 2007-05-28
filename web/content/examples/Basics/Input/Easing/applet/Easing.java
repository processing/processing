import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Easing extends PApplet {/**
 * Easing. 
 * 
 * Move the mouse across the screen and the symbol will follow.  
 * Between drawing each frame of the animation, the program
 * calculates the difference between the position of the 
 * symbol and the cursor. If the distance is larger than
 * 1 pixel, the symbol moves half of the distance from its
 * current position toward the cursor. 
 * 
 * Updated 21 August 2002
 */
 
float x;
float y;
float targetX, targetY;
float easing = 0.05f;

public void setup() 
{
  size(200, 200); 
  noStroke();  
}

public void draw() 
{ 
  background( 51 );
  
  targetX = mouseX;
  float dx = mouseX - x;
  if(abs(dx) > 1) {
    x += dx * easing;
  }
  
  targetY = mouseY;
  float dy = mouseY - y;
  if(abs(dy) > 1) {
    y += dy * easing;
  }
  
  translate( x, y );
  plus();
}

public void plus() 
{
  int s = 20;
  int t = 3;
  // Draw the symbol
  beginShape();
  vertex( -t,  s );
  vertex(  t,  s );
  vertex(  t,  t );
  vertex(  s,  t );
  vertex(  s, -t );
  vertex(  t, -t );
  vertex(  t, -s );
  vertex( -t, -s );
  vertex( -t, -t );
  vertex( -s, -t );
  vertex( -s,  t );
  vertex( -t,  t );  
  endShape(CLOSE);
}
static public void main(String args[]) {   PApplet.main(new String[] { "Easing" });}}