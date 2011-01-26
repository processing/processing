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

public class MultipleConstructors extends PApplet {

/**
 * Multiple constructors
 * 
 * A class can have multiple constructors that assign the fields in different ways. 
 * Sometimes it's beneficial to specify every aspect of an object's data by assigning 
 * parameters to the fields, but other times it might be appropriate to define only 
 * one or a few.
 */

Spot sp1, sp2;
public void setup() 
{
  size(200, 200);
  background(204);
  smooth();
  noLoop();
  // Run the constructor without parameters
  sp1 = new Spot();
  // Run the constructor with three parameters
  sp2 = new Spot(122, 100, 40);
}

public void draw() {
  sp1.display();
  sp2.display();
}

class Spot {
  float x, y, radius;
  // First version of the Spot constructor;
  // the fields are assigned default values
  Spot() {
    x = 66;
    y = 100;
    radius = 16;
  }
  // Second version of the Spot constructor;
  // the fields are assigned with parameters
  Spot(float xpos, float ypos, float r) {
    x = xpos;
    y = ypos;
    radius = r;
  }
  public void display() {
    ellipse(x, y, radius*2, radius*2);
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "MultipleConstructors" });
  }
}
