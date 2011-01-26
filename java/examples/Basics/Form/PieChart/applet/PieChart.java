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

public class PieChart extends PApplet {
  public void setup() {/**
 * Pie Chart  
 * By Ira Greenberg 
 * 
 * Uses the arc() function to generate a pie chart from the data
 * stored in an array. 
 */
 
size(200, 200);
background(100);
smooth();
noStroke();

int diameter = 150;
int[] angs = {30, 10, 45, 35, 60, 38, 75, 67};
float lastAng = 0;

for (int i = 0; i < angs.length; i++){
  fill(angs[i] * 3.0f);
  arc(width/2, height/2, diameter, diameter, lastAng, lastAng+radians(angs[i]));
  lastAng += radians(angs[i]);  
}


  noLoop();
} 
  static public void main(String args[]) {
    PApplet.main(new String[] { "PieChart" });
  }
}
