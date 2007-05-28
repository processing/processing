import processing.core.*; import processing.opengl.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Lights extends PApplet {/**
 * Lights. 
 * Modified by an example by Simon Greenwold. 
 *
 * Display a box with three different kinds of lights. 
 *
 * Created 27 May 2007.
 */



public void setup() 
{
  size(800, 600, OPENGL);
  noStroke();
}

public void draw() 
{
  defineLights();
  background(0);
  
  for (int x = 0; x <= width; x += 100) {
    for (int y = 0; y <= height; y += 100) {
      pushMatrix();
      translate(x, y);
      rotateY(map(mouseX, 0, width, 0, PI));
      rotateX(map(mouseY, 0, height, 0, PI));
      box(90);
      popMatrix();
    }
  }
}

public void defineLights() {
  // Orange point light on the right
  pointLight(150, 100, 0,   // Color
             200, -150, 0); // Position

  // Blue directional light from the left
  directionalLight(0, 102, 255, // Color
                   1, 0, 0);    // The x-, y-, z-axis direction

  // Yellow spotlight from the front
  spotLight(255, 255, 109,  // Color
            0, 40, 200,     // Position
            0, -0.5f, -0.5f,  // Direction
            PI / 2, 2);     // Angle, concentration
}
static public void main(String args[]) {   PApplet.main(new String[] { "Lights" });}}