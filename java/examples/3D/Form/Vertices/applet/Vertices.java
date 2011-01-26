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

/**
 * Vertices 
 * by Simon Greenwold.
 * 
 * Draw a cylinder centered on the y-axis, going down 
 * from y=0 to y=height. The radius at the top can be 
 * different from the radius at the bottom, and the 
 * number of sides drawn is variable.
 */

public void setup() {
  size(640, 360, P3D);
}

public void draw() {
  background(0);
  lights();
  translate(width / 2, height / 2);
  rotateY(map(mouseX, 0, width, 0, PI));
  rotateZ(map(mouseY, 0, height, 0, -PI));
  noStroke();
  fill(255, 255, 255);
  translate(0, -40, 0);
  drawCylinder(10, 180, 200, 16); // Draw a mix between a cylinder and a cone
  //drawCylinder(70, 70, 120, 64); // Draw a cylinder
  //drawCylinder(0, 180, 200, 4); // Draw a pyramid
}

public void drawCylinder(float topRadius, float bottomRadius, float tall, int sides) {
  float angle = 0;
  float angleIncrement = TWO_PI / sides;
  beginShape(QUAD_STRIP);
  for (int i = 0; i < sides + 1; ++i) {
    vertex(topRadius*cos(angle), 0, topRadius*sin(angle));
    vertex(bottomRadius*cos(angle), tall, bottomRadius*sin(angle));
    angle += angleIncrement;
  }
  endShape();
  
  // If it is not a cone, draw the circular top cap
  if (topRadius != 0) {
    angle = 0;
    beginShape(TRIANGLE_FAN);
    
    // Center point
    vertex(0, 0, 0);
    for (int i = 0; i < sides + 1; i++) {
      vertex(topRadius * cos(angle), 0, topRadius * sin(angle));
      angle += angleIncrement;
    }
    endShape();
  }

  // If it is not a cone, draw the circular bottom cap
  if (bottomRadius != 0) {
    angle = 0;
    beginShape(TRIANGLE_FAN);

    // Center point
    vertex(0, tall, 0);
    for (int i = 0; i < sides + 1; i++) {
      vertex(bottomRadius * cos(angle), tall, bottomRadius * sin(angle));
      angle += angleIncrement;
    }
    endShape();
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Vertices" });
  }
}
