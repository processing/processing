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

public class CompositeObjects extends PApplet {

/**
 * Composite Objects
 * 
 * An object can include several other objects. Creating such composite objects 
 * is a good way to use the principles of modularity and build higher levels of 
 * abstraction within a program.
 */

EggRing er1, er2;

public void setup() 
{
  size(200, 200);
  smooth();
  er1 = new EggRing(66, 132, 0.1f, 66);
  er2 = new EggRing(132, 180, 0.05f, 132);
}

public void draw() 
{
  background(0);
  er1.transmit();
  er2.transmit();
}

class EggRing 
{
  Egg ovoid;
  Ring circle = new Ring();
  EggRing(int x, int y, float t, float sp) {
    ovoid = new Egg(x, y, t, sp);
    circle.start(x, y - sp/2);
  }
  public void transmit() {
    ovoid.wobble();
    ovoid.display();
    circle.grow();
    circle.display();
    if (circle.on == false) {
      circle.on = true;
    }
  }
}

class Egg {
  float x, y; // X-coordinate, y-coordinate
  float tilt; // Left and right angle offset
  float angle; // Used to define the tilt
  float scalar; // Height of the egg
  // Constructor
  Egg(int xpos, int ypos, float t, float s) {
    x = xpos;
    y = ypos;
    tilt = t;
    scalar = s / 100.0f;
  }
  public void wobble() {
    tilt = cos(angle) / 8;
    angle += 0.1f;
  }
  public void display() {
    noStroke();
    fill(255);
    pushMatrix();
    translate(x, y);
    rotate(tilt);
    scale(scalar);
    beginShape();
    vertex(0, -100);
    bezierVertex(25, -100, 40, -65, 40, -40);
    bezierVertex(40, -15, 25, 0, 0, 0);
    bezierVertex(-25, 0, -40, -15, -40, -40);
    bezierVertex(-40, -65, -25, -100, 0, -100);
    endShape();
    popMatrix();
  }
}

class Ring {
  float x, y; // X-coordinate, y-coordinate
  float diameter; // Diameter of the ring
  boolean on = false; // Turns the display on and off
  public void start(float xpos, float ypos) {
    x = xpos;
    y = ypos;
    on = true;
    diameter = 1;
  }
  public void grow() {
    if (on == true) {
      diameter += 0.5f;
      if (diameter > width*2) {
        diameter = 0.0f;
      }
    }
  }
  public void display() {
    if (on == true) {
      noFill();
      strokeWeight(4);
      stroke(155, 153);
      ellipse(x, y, diameter, diameter);
    }
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "CompositeObjects" });
  }
}
