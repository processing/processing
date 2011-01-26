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

public class IncrementDecrement extends PApplet {

/**
 * Increment Decrement. 
 * 
 * Writing "a++" is equivalent to "a = a + 1".  
 * Writing "a--" is equivalent to "a = a - 1".   
 */
 
int a;
int b;
boolean direction;

public void setup()
{
  size(200, 200);
  colorMode(RGB, width);
  a = 0;
  b = width;
  direction = true;
  frameRate(30);
}

public void draw()
{
  a++;
  if(a > width) {
    a = 0;
    direction = !direction;
  }
  if(direction == true){
    stroke(a);
  } else {
    stroke(width-a);
  }
  line(a, 0, a, height/2);

  b--;
  if(b < 0) {
    b = width;
  }
  if(direction == true) {
    stroke(width-b);
  } else {
    stroke(b);
  }
  line(b, height/2+1, b, height);
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "IncrementDecrement" });
  }
}
