import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class SaveFile1 extends PApplet {/**
 * SaveFile 1
 *
 * Saving files is a useful way to store data so it can be viewed after a 
 * program has stopped running. The saveStrings() function writes an array 
 * of strings to a file, with each string written to a new line. This file 
 * is saved to the sketch\u2019s folder
 *
 * Created 26 May 2007
 */

int[] x = new int[0];
int[] y = new int[0];

public void setup() 
{
  size(200, 200);
}

public void draw() 
{
  background(204);
  stroke(0);
  noFill();
  beginShape();
  for (int i = 0; i < x.length; i++) {
    vertex(x[i], y[i]);
  }
  endShape();
  // Show the next segment to be added
  if (x.length >= 1) {
    stroke(255);
    line(mouseX, mouseY, x[x.length-1], y[x.length-1]);
  }
}

public void mousePressed() { // Click to add a line segment
  x = append(x, mouseX);
  y = append(y, mouseY);
}

public void keyPressed() { // Press a key to save the data
  String[] lines = new String[x.length];
  for (int i = 0; i < x.length; i++) {
    lines[i] = x[i] + "\t" + y[i];
  }
  saveStrings("lines.txt", lines);
  //exit(); // Stop the program
}
static public void main(String args[]) {   PApplet.main(new String[] { "SaveFile1" });}}