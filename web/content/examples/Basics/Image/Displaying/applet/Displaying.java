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

public class Displaying extends PApplet {
  public void setup() {/**
 * Displaying. 
 * 
 * Images can be displayed to the screen at their actual size
 * or any other size. 
 */
 
size(200, 200);
PImage a;  // Declare variable "a" of type PImage
a = loadImage("arch.jpg"); // Load the images into the program
image(a, 0, 0); // Displays the image from point (0,0)
image(a, width/2, 0, a.width/2, a.height/2);

  noLoop();
} 
  static public void main(String args[]) {
    PApplet.main(new String[] { "Displaying" });
  }
}
