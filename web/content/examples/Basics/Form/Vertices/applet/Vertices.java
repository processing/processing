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

public class Vertices extends PApplet {
  public void setup() {/**
 * Vertices. 
 * 
 * The beginShape() function begins recording vertices 
 * for a shape and endShape() stops recording. 
 * A vertex is a location in space specified by X, Y, 
 * and sometimes Z coordinates. After calling the beginShape() function, 
 * a series of vertex() functions must follow.  
 * To stop drawing the shape, call the endShape() functions.
 */
 
size(200, 200);
background(0);
noFill();

stroke(102);
beginShape();
curveVertex(168, 182);
curveVertex(168, 182);
curveVertex(136, 38);
curveVertex(42, 34);
curveVertex(64, 200);
curveVertex(64, 200);
endShape();

stroke(51);
beginShape(LINES);
vertex(60, 40);
vertex(160, 10);
vertex(170, 150);
vertex(60, 150);
endShape();

stroke(126);
beginShape();
vertex(60, 40);
bezierVertex(160, 10, 170, 150, 60, 150);
endShape();

stroke(255);
beginShape(POINTS);
vertex(60, 40);
vertex(160, 10);
vertex(170, 150);
vertex(60, 150);
endShape();


  noLoop();
} 
  static public void main(String args[]) {
    PApplet.main(new String[] { "Vertices" });
  }
}
