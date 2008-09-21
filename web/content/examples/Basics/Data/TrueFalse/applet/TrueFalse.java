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

public class TrueFalse extends PApplet {
  public void setup() {/**
 * True/False. 
 * 
 * Boolean data is one bit of information. True or false. 
 * It is common to use Booleans with control statements to 
 * determine the flow of a program. In this example, when the
 * boolean value "x" is true, vertical black lines are drawn and when
 * the boolean value "x" is false, horizontal gray lines are drawn. 
 */
 
boolean x = false;

size(200, 200);
background(0);
stroke(0);

for (int i = 1; i < width; i += 2) 
{
  if (i < width/2) {
    x = true;
  } else {
    x = false;
  }
  
  if (x) {
    stroke(255);
    line(i, 1, i, height-1);
  }
  
  if (!x) {
    stroke(126);
    line(width/2 , i, width-2, i);
  }
}

  noLoop();
} 
  static public void main(String args[]) {
    PApplet.main(new String[] { "TrueFalse" });
  }
}
