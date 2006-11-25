import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class ContinuousLines extends PApplet {// Continuous Lines
// by REAS <http://reas.com>

// Click and drag the mouse to draw a line.

// Updated 27 October 2002 
// Created 23 October 2002

public void setup() {
  size(200, 200);
  background(102);
}

public void draw() {
  stroke(255);
  if(mousePressed) {
    line(mouseX, mouseY, pmouseX, pmouseY);
  }
}
}