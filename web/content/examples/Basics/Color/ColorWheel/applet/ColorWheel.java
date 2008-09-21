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

public class ColorWheel extends PApplet {

/**
 * Subtractive Color Wheel 
 * by Ira Greenberg. 
 * 
 * The primaries are red, yellow, and blue. The
 * secondaries are green, purple, and orange. The 
 * tertiaries are  yellow-orange, red-orange, red-purple, 
 * blue-purple, blue-green, and yellow-green.
 * 
 * Create a shade or tint of the 
 * subtractive color wheel using
 * SHADE or TINT parameters.
 */

int segs = 12;
int steps = 6;
float rotAdjust = radians(360.0f/segs/2.0f);
float radius = 95.0f;
float segWidth = radius/steps;
float interval = TWO_PI/segs;
int SHADE = 0;
int TINT = 1;

public void setup(){
  size(200, 200);
  background(127);
  smooth();
  ellipseMode(CENTER_RADIUS);
  noStroke();
 // you can substitue TINT for SHADE argument
 createWheel(width/2, height/2, SHADE);
}

public void createWheel(int x, int y, int valueShift){
  if (valueShift == SHADE){
    for (int j=0; j<steps; j++){
      int[]cols = { 
        color(255-(255/steps)*j, 255-(255/steps)*j, 0), 
        color(255-(255/steps)*j, (255/1.5f)-((255/1.5f)/steps)*j, 0), 
        color(255-(255/steps)*j, (255/2)-((255/2)/steps)*j, 0), 
        color(255-(255/steps)*j, (255/2.5f)-((255/2.5f)/steps)*j, 0), 
        color(255-(255/steps)*j, 0, 0), 
        color(255-(255/steps)*j, 0, (255/2)-((255/2)/steps)*j), 
        color(255-(255/steps)*j, 0, 255-(255/steps)*j), 
        color((255/2)-((255/2)/steps)*j, 0, 255-(255/steps)*j), 
        color(0, 0, 255-(255/steps)*j),
        color(0, 255-(255/steps)*j, (255/2.5f)-((255/2.5f)/steps)*j), 
        color(0, 255-(255/steps)*j, 0), 
        color((255/2)-((255/2)/steps)*j, 255-(255/steps)*j, 0) };
      for (int i=0; i< segs; i++){
        fill(cols[i]);
        arc(x, y, radius, radius, interval*i+rotAdjust, interval*(i+1)+rotAdjust);
      }
      radius -= segWidth;
    }
  } else  if (valueShift == TINT){
    for (int j=0; j<steps; j++){
      int[]cols = { 
        color((255/steps)*j, (255/steps)*j, 0), 
        color((255/steps)*j, ((255/1.5f)/steps)*j, 0), 
        color((255/steps)*j, ((255/2)/steps)*j, 0), 
        color((255/steps)*j, ((255/2.5f)/steps)*j, 0), 
        color((255/steps)*j, 0, 0), 
        color((255/steps)*j, 0, ((255/2)/steps)*j), 
        color((255/steps)*j, 0, (255/steps)*j), 
        color(((255/2)/steps)*j, 0, (255/steps)*j), 
        color(0, 0, (255/steps)*j),
        color(0, (255/steps)*j, ((255/2.5f)/steps)*j), 
        color(0, (255/steps)*j, 0), 
        color(((255/2)/steps)*j, (255/steps)*j, 0) };
      for (int i=0; i< segs; i++){
        fill(cols[i]);
        arc(x, y, radius, radius, interval*i+rotAdjust, interval*(i+1)+rotAdjust);
      }
      radius -= segWidth;
    }
  } 
}


  static public void main(String args[]) {
    PApplet.main(new String[] { "ColorWheel" });
  }
}
