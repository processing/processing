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

public class Conditionals1 extends PApplet {
  public void setup() {/**
 * Conditionals 1. 
 * 
 * Conditions are like questions. 
 * They allow a program to decide to take one action if 
 * the answer to a question is true or to do another action
 * if the answer to the question is false. 
 * The questions asked within a program are always logical
 * or relational statements. For example, if the variable 'i' is 
 * equal to zero then draw a line. 
 */
 
size(200, 200);
background(0);

for(int i=10; i<width; i+=10) {
  // If 'i' divides by 20 with no remainder draw the first line
  // else draw the second line
  if(i%20 == 0) {
    stroke(153);
    line(i, 40, i, height/2);
  } else {
    stroke(102);
    line(i, 20, i, 180); 
  }
}

  noLoop();
} 
  static public void main(String args[]) {
    PApplet.main(new String[] { "Conditionals1" });
  }
}
