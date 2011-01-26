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

public class IntegersFloats extends PApplet {

/**
 * Integers Floats. 
 * 
 * Integers and floats are two different kinds of numerical data. 
 * An integer (more commonly called an int) is a number without 
 * a decimal point. A float is a floating-point number, which means 
 * it is a number that has a decimal place. Floats are used when
 * more precision is needed. 
 */
 
int a = 0;      // Create a variable "a" of the datatype "int"
float b = 0.0f;  // Create a variable "b" of the datatype "float"

public void setup()
{
  size(200, 200);
  stroke(255);
  frameRate(30);
}

public void draw()
{
  background(51);
  
  a = a + 1;
  b = b + 0.2f; 
  line(a, 0, a, height/2);
  line(b, height/2, b, height);
  
  if(a > width) {
    a = 0;
  }
  if(b > width) {
    b = 0;
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "IntegersFloats" });
  }
}
