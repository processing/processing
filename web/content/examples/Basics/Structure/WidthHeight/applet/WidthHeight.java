import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class WidthHeight extends PApplet {public void setup() {// Width and Height
// by REAS <http://reas.com>

// The 'width' and 'height' variables contain the width and height 
// of the display window as defined in the size() function.

// Created 27 October 2002

size(200, 200);
background(127);
noStroke();
for(int i=0; i<height; i+=20) {
  fill(0);
  rect(0, i, width, 10);
  fill(255);
  rect(i, 0, 10, height);
}
noLoop(); }}