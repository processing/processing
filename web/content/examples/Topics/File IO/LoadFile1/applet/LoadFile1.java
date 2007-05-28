import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class LoadFile1 extends PApplet {/**
 * LoadFile 1
 * 
 * Loads a text file that contains two numbers separated by a tab ('\t').
 * A new pair of numbers is loaded each frame and used to draw a point on the screen.
 */

String[] lines;
int index = 0;

public void setup() {
  size(200, 200);
  background(0);
  stroke(255);
  frameRate(12);
  lines = loadStrings("positions.txt");
}

public void draw() {
  if (index < lines.length) {
    String[] pieces = split(lines[index], '\t');
    if (pieces.length == 2) {
      int x = PApplet.parseInt(pieces[0]) * 2;
      int y = PApplet.parseInt(pieces[1]) * 2;
      point(x, y);
    }
    // Go to the next line for the next run through draw()
    index = index + 1;
  }
}
static public void main(String args[]) {   PApplet.main(new String[] { "LoadFile1" });}}