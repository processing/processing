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

public class ScaleShape extends PApplet {

/**
 * Scale Shape.  
 * Illustration by George Brower. 
 * 
 * Move the mouse left and right to zoom the SVG file.
 * This shows how, unlike an imported image, the lines
 * remain smooth at any size.
 */

PShape bot;

public void setup() {
  size(640, 360);
  smooth();
  // The file "bot1.svg" must be in the data folder
  // of the current sketch to load successfully
  bot = loadShape("bot1.svg");
} 

public void draw() {
  background(102);
  translate(width/2, height/2);
  float zoom = map(mouseX, 0, width, 0.1f, 4.5f);
  scale(zoom);
  shape(bot, -140, -140);
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "ScaleShape" });
  }
}
