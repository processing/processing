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

public class Pulses extends PApplet {

/**
 * Pulses. 
 * 
 * Software drawing instruments can follow a rhythm or abide by rules independent
 * of drawn gestures. This is a form of collaborative drawing in which the draftsperson
 * controls some aspects of the image and the software controls others.
 */

int angle = 0;

public void setup() {
  size(640, 360);
  background(102);
  smooth();
  noStroke();
  fill(0, 102);
}

public void draw() {
  // Draw only when mouse is pressed
  if (mousePressed == true) {
    angle += 10;
    float val = cos(radians(angle)) * 6.0f;
    for (int a = 0; a < 360; a += 75) {
      float xoff = cos(radians(a)) * val;
      float yoff = sin(radians(a)) * val;
      fill(0);
      ellipse(mouseX + xoff, mouseY + yoff, val, val);
    }
    fill(255);
    ellipse(mouseX, mouseY, 2, 2);
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Pulses" });
  }
}
