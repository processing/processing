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

public class MousePress extends PApplet {

/**
 * Click. 
 * 
 * Move the mouse to position the shape. 
 * Press the mouse button to invert the color. 
 */


public void setup() {
  size(200, 200);
  fill(126);
  background(102);
}

public void draw() {
  if(mousePressed) {
    stroke(255);
  } else {
    stroke(0);
  }
  line(mouseX-66, mouseY, mouseX+66, mouseY);
  line(mouseX, mouseY-66, mouseX, mouseY+66); 
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "MousePress" });
  }
}
