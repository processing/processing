import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Interpolate extends PApplet {// Interpolate
// by REAS <http://reas.com>

// Move the mouse across the screen and the symbol will follow. 
// Between drawing each frame of the animation, the program
// calculates the difference between the position of the 
// symbol and the cursor. If the distance is larger than
// 1 pixel, the symbol moves half of the distance from its
// current position toward the cursor.

// Updated 21 August 2002

float mx;
float my;
float delay = 60.0f;

public void setup() 
{
  size(200, 200); 
  noStroke();  
}

public void draw() 
{ 
  background( 51 );
  
  float dx = mouseX - mx;
  if(abs(dx) > 1) {
    mx = mx + dx/delay;
  }
  float dy = mouseY - my;
  if(abs(dy) > 1) {
    my = my + dy/delay;
  }
  
  translate( mx, my );
  plus();
}

public void plus() 
{
  int s = 20;
  int t = 3;
  
  // Draw the symbol
  beginShape( POLYGON );
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
  endShape();
}
}