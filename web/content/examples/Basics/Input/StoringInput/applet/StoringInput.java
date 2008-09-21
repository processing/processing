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

public class StoringInput extends PApplet {

/**
 * Storing Input. 
 * 
 * Move the mouse across the screen to change the position
 * of the circles. The positions of the mouse are recorded
 * into an array and played back every frame. Between each
 * frame, the newest value are added to the end of each array
 * and the oldest value is deleted. 
 */
 
int num = 60;
float mx[] = new float[num];
float my[] = new float[num];

public void setup() 
{
  size(200, 200);
  smooth();
  noStroke();
  fill(255, 153); 
}

public void draw() 
{
  background(51); 
  
  // Reads throught the entire array
  // and shifts the values to the left
  for(int i=1; i<num; i++) {
    mx[i-1] = mx[i];
    my[i-1] = my[i];
  } 
  // Add the new values to the end of the array
  mx[num-1] = mouseX;
  my[num-1] = mouseY;
  
  for(int i=0; i<num; i++) {
    ellipse(mx[i], my[i], i/2, i/2);
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "StoringInput" });
  }
}
