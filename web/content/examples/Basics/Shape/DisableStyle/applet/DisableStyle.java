import processing.core.*; 
import processing.xml.*; 

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

public class DisableStyle extends PApplet {

/**
 * Ignore Styles. 
 * Illustration by George Brower. 
 * 
 * Shapes are loaded with style information that tells them how
 * to draw (the color, stroke weight, etc.) The disableStyle() 
 * method of PShape turns off this information. The enableStyle()
 * method turns it back on.
 */

PShape bot;

public void setup() {
  size(640, 360);
  smooth();
  // The file "bot1.svg" must be in the data folder
  // of the current sketch to load successfully
  bot = loadShape("bot1.svg");
  noLoop();
} 

public void draw() {
  background(102);
  
  // Draw left bot
  bot.disableStyle();  // Ignore the colors in the SVG
  fill(0, 102, 153);    // Set the SVG fill to blue
  stroke(255);          // Set the SVG fill to white
  shape(bot, 20, 25, 300, 300);

  // Draw right bot
  bot.enableStyle();
  shape(bot, 320, 25, 300, 300);
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "DisableStyle" });
  }
}
