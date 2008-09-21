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

public class Chain extends PApplet {

/**
 * Chain. 
 * 
 * One mass is attached to the mouse position and the other 
 * is attached the position of the other mass. The gravity
 * in the environment pulls down on both. 
 */


Spring2D s1, s2;

float gravity = 6.0f;
float mass = 2.0f;

public void setup() 
{
  size(200, 200);
  smooth();
  fill(0);
  // Inputs: x, y, mass, gravity
  s1 = new Spring2D(0.0f, width/2, mass, gravity);
  s2 = new Spring2D(0.0f, width/2, mass, gravity);
}

public void draw() 
{
  background(204);
  s1.update(mouseX, mouseY);
  s1.display(mouseX, mouseY);
  s2.update(s1.x, s1.y);
  s2.display(s1.x, s1.y);
}

class Spring2D {
  float vx, vy; // The x- and y-axis velocities
  float x, y; // The x- and y-coordinates
  float gravity;
  float mass;
  float radius = 20;
  float stiffness = 0.2f;
  float damping = 0.7f;
  
  Spring2D(float xpos, float ypos, float m, float g) {
    x = xpos;
    y = ypos;
    mass = m;
    gravity = g;
  }
  
  public void update(float targetX, float targetY) {
    float forceX = (targetX - x) * stiffness;
    float ax = forceX / mass;
    vx = damping * (vx + ax);
    x += vx;
    float forceY = (targetY - y) * stiffness;
    forceY += gravity;
    float ay = forceY / mass;
    vy = damping * (vy + ay);
    y += vy;
  }
  
  public void display(float nx, float ny) {
    noStroke();
    ellipse(x, y, radius*2, radius*2);
    stroke(255);
    line(x, y, nx, ny);
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Chain" });
  }
}
