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

public class Milliseconds extends PApplet {

/**
 * Milliseconds. 
 * 
 * A millisecond is 1/1000 of a second. 
 * Processing keeps track of the number of milliseconds a program has run.
 * By modifying this number with the modulo(%) operator, 
 * different patterns in time are created.  
 */
 
float scale;

public void setup()
{
  size(200, 200);
  noStroke();
  scale = width/10;
}

public void draw()
{ 
  for(int i=0; i<scale; i++) {
    colorMode(RGB, (i+1) * scale * 10);
    fill(millis()%((i+1) * scale * 10) );
    rect(i*scale, 0, scale, height);
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Milliseconds" });
  }
}
