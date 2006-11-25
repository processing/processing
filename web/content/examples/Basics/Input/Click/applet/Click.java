import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Click extends PApplet {// Click
// by REAS <http://reas.com>

// Move the mouse to position the shape.
// Press the mouse button to invert the color.

// Updated 21 August 2002

int size = 30;

public void setup() {
  size(200, 200);
  ellipseMode(CENTER);
  fill(126);
  noStroke();
  rect(0, 0, width, height);
}

public void draw() {
  if(mousePressed) {
    stroke(255);
  } else {
    stroke(51);
  }
  line(mouseX-30, mouseY, mouseX+30, mouseY);
  line(mouseX, mouseY-30, mouseX, mouseY+30); 
}
}