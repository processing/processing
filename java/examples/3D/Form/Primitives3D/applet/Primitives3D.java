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

public class Primitives3D extends PApplet {
  public void setup() {/**
 * Primitives 3D. 
 * 
 * Placing mathematically 3D objects in synthetic space.
 * The lights() method reveals their imagined dimension.
 * The box() and sphere() functions each have one parameter
 * which is used to specify their size. These shapes are
 * positioned using the translate() function.
 */
 
size(640, 360, P3D); 
background(0);
lights();

noStroke();
pushMatrix();
translate(130, height/2, 0);
rotateY(1.25f);
rotateX(-0.4f);
box(100);
popMatrix();

noFill();
stroke(255);
pushMatrix();
translate(500, height*0.35f, -200);
sphere(280);
popMatrix();



  noLoop();
} 
  static public void main(String args[]) {
    PApplet.main(new String[] { "Primitives3D" });
  }
}
