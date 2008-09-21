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

public class Loop extends PApplet {

/**
 * Loop. 
 * 
 * The loop() function causes draw() to execute
 * continuously. If noLoop is called in setup()
 * the draw() is only executed once. In this example
 * click the mouse to execute loop(), which will
 * cause the draw() the execute continuously. 
 */
 
// The statements in the setup() function 
// execute once when the program begins
public void setup() 
{
  size(200, 200);  // Size should be the first statement
  stroke(255);     // Set stroke color to white
  noLoop();
}

float y = 100;

// The statements in draw() are run until the 
// program is stopped. Each statement is run in 
// sequence and after the last line is read, the first 
// line is run again.
public void draw() 
{ 
  background(0);   // Set the background to black
  line(0, y, width, y);  
  
  y = y - 1; 
  if (y < 0) { 
    y = height; 
  } 
} 

public void mousePressed() 
{
  loop();
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Loop" });
  }
}
