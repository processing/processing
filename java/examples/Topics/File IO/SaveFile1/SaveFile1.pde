/**
 * SaveFile 1
 * 
 * Saving files is a useful way to store data so it can be viewed after a 
 * program has stopped running. The saveStrings() function writes an array 
 * of strings to a file, with each string written to a new line. This file 
 * is saved to the sketch's folder. This example won't work in a web browser
 * because of Java security restrictions.
 */

int[] x = new int[0];
int[] y = new int[0];

void setup() 
{
  size(200, 200);
}

void draw() 
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

void mousePressed() { // Click to add a line segment
  x = append(x, mouseX);
  y = append(y, mouseY);
}

void keyPressed() { // Press a key to save the data
  String[] lines = new String[x.length];
  for (int i = 0; i < x.length; i++) {
    lines[i] = x[i] + "\t" + y[i];
  }
  saveStrings("lines.txt", lines);
  exit(); // Stop the program
}

