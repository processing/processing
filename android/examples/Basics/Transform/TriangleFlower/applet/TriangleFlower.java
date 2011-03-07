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

public class TriangleFlower extends PApplet {

/**
 * Triangle Flower 
 * by Ira Greenberg. 
 * 
 * Using rotate() and triangle() functions generate a pretty 
 * flower. Uncomment the line "// rotate(rot+=radians(spin));"
 * in the triBlur() function for a nice variation.
 */

Point[]p = new Point[3];
float shift = 1.0f;
float fade = 0;
float fillCol = 0;
float rot = 0;
float spin = 0;

public void setup(){
  size(200, 200);
  background(0);
  smooth();
  fade = 255.0f/(width/2.0f/shift);
  spin = 360.0f/(width/2.0f/shift);
  p[0] = new Point(-width/2, height/2);
  p[1] = new Point(width/2, height/2);
  p[2] = new Point(0, -height/2);
  noStroke();
  translate(width/2, height/2);
  triBlur();
}

public void triBlur(){
  fill(fillCol);
  fillCol+=fade;
  rotate(spin);
  // another interesting variation: uncomment the line below 
  // rotate(rot+=radians(spin));
  triangle(p[0].x+=shift, p[0].y-=shift/2, p[1].x-=shift, p[1].y-=shift/2, p[2].x, p[2].y+=shift); 
  if(p[0].x<0){
    // recursive call
    triBlur();
  }
}


  static public void main(String args[]) {
    PApplet.main(new String[] { "TriangleFlower" });
  }
}
