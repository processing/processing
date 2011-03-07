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

public class Creating extends PApplet {
  public void setup() {/**
 * Creating Colors (Homage to Albers). 
 * 
 * Creating variables for colors that may be referred to 
 * in the program by their name, rather than a number. 
 */

size(200, 200);
noStroke();

int inside = color(204, 102, 0);
int middle = color(204, 153, 0);
int outside = color(153, 51, 0);

// These statements are equivalent to the statements above.
// Programmers may use the format they prefer.
//color inside = #CC6600;
//color middle = #CC9900;
//color outside = #993300;

fill(outside);
rect(0, 0, 200, 200);
fill(middle);
rect(40, 60, 120, 120);
fill(inside);
rect(60, 90, 80, 80);

  noLoop();
} 
  static public void main(String args[]) {
    PApplet.main(new String[] { "Creating" });
  }
}
