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

public class Iteration extends PApplet {
  public void setup() {/**
 * Iteration. 
 * 
 * Iteration with a "for" structure constructs repetitive forms. 
 */
 
int k;
int xpos1 = 100; 
int xpos2 = 118; 
int count = 0; 
int timey = 0;
int num = 12;

size(200, 200);
background(102);
noStroke();
 
// Draw gray bars 
fill(255);
k=60;
for(int i=0; i < num/3; i++) {
  rect(25, k, 155, 5);
  k+=10;
}

// Black bars
fill(51);
k = 40;
for(int i=0; i < num; i++) {
  rect(105, k, 30, 5);
  k += 10;
}
k = 15;
for(int i = 0; i < num; i++) {
  rect(125, k, 30, 5);
  k +=10;
}
  
// Thin lines
k = 42;
fill(0);
for(int i=0; i < num-1; i++) {
  rect(36, k, 20, 1);
  k+=10;
}

  noLoop();
} 
  static public void main(String args[]) {
    PApplet.main(new String[] { "Iteration" });
  }
}
