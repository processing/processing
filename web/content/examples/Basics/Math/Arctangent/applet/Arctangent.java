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

public class Arctangent extends PApplet {

/**
 * Arctangent. 
 * 
 * Move the mouse to change the direction of the eyes. 
 * The atan2() function computes the angle from each eye 
 * to the cursor. 
 */
 
Eye e1, e2, e3, e4, e5;

public void setup() 
{
  size(200, 200);
  smooth();
  noStroke();
  e1 = new Eye( 50,  16,  80);
  e2 = new Eye( 64,  85,  40);  
  e3 = new Eye( 90, 200, 120);
  e4 = new Eye(150,  44,  40); 
  e5 = new Eye(175, 120,  80);
}

public void draw() 
{
  background(102);
  
  e1.update(mouseX, mouseY);
  e2.update(mouseX, mouseY);
  e3.update(mouseX, mouseY);
  e4.update(mouseX, mouseY);
  e5.update(mouseX, mouseY);

  e1.display();
  e2.display();
  e3.display();
  e4.display();
  e5.display();
}

class Eye 
{
  int ex, ey;
  int size;
  float angle = 0.0f;
  
  Eye(int x, int y, int s) {
    ex = x;
    ey = y;
    size = s;
 }

  public void update(int mx, int my) {
    angle = atan2(my-ey, mx-ex);
  }
  
  public void display() {
    pushMatrix();
    translate(ex, ey);
    fill(255);
    ellipse(0, 0, size, size);
    rotate(angle);
    fill(153);
    ellipse(size/4, 0, size/2, size/2);
    popMatrix();
  }
}


  static public void main(String args[]) {
    PApplet.main(new String[] { "Arctangent" });
  }
}
