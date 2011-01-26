import processing.core.*; 
import processing.xml.*; 

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

public class RunAmuck extends PApplet {

/**
 * Run-Amuck
 * By Ira Greenberg <br />
 * Processing for Flash Developers,
 * Friends of ED, 2009
 */

int count = 250;
Legs[] legs = new Legs[count];

public void setup() {
  size(640, 360, P3D);
  noStroke();
  for (int i = 0; i < legs.length; i++) {
    legs[i] = new Legs(random(-10, 10), random(-50, 150), random(.5f, 5), 
                       random(.5f, 5), color(random(255), random(255), random(255)));
  }
}

public void draw() {
  background(0);
  translate(width/2, height/2);
  noStroke();
  fill(35);

  // Draw ground plane
  beginShape();
  vertex(-width*2, 0, -1000);
  vertex(width*2, 0, -1000);
  vertex(width/2, height/2, 400);
  vertex(-width/2, height/2, 400);
  endShape(CLOSE);

  // Update and draw the legs
  for (int i = 0; i < legs.length; i++) {
    legs[i].create();
    // Set foot step rate
    legs[i].step(random(10, 50));
    // Move legs along x, y, z axes
    // z-movement dependent upon step rate
    legs[i].move();
  }
}


/**
 * Legs class
 * By Ira Greenberg <br />
 * Processing for Flash Developers,
 * Friends of ED, 2009
 */
 
class Legs {
  // Instance properties with default values
  float x = 0, y = 0, z = 0, w = 150, ht = 125;
  int col = 0xff77AA22;
  // Advanced properties
  float detailW = w/6.0f;
  float detailHt = ht/8.0f;
  float shoeBulge = detailHt*2.0f;
  float legGap = w/7.0f;

  // Dynamics properties
  float velocity = .02f, stepL, stepR, stepRate = random(10, 50); 
  float speedX = 1.0f, speedZ, spring, damping = .5f, theta;

  // Default constructor
  Legs() {
  }

  // Standard constructor
  Legs(float x, float z, float w, float ht, int col) {
    this.x = x;
    this.z = z;
    this.w = w;
    this.ht = ht;
    this.col = col;
    fill(col);
    detailW = w/6.0f;
    detailHt = ht/8.0f;
    shoeBulge = detailHt*2.0f;
    legGap = w/7.0f;
    speedX = random(-speedX, speedX);
  }

  // Advanced constructor
  Legs(float x, float z, float w, float ht, int col, float detailW, 
        float detailHt, float shoeBulge, float legGap) {
    this.x = x;
    this.z = z;
    this.w = w;
    this.ht = ht;
    this.col = col;
    this.detailW = detailW;
    this.detailHt = detailHt;
    this.shoeBulge = shoeBulge;
    this.legGap = legGap;
    speedX = random(-speedX, speedX);
  }

  // Draw legs
  public void create() {
    fill(col);
    float footWidth = (w - legGap)/2;
    beginShape();
    vertex(x - w/2, y - ht, z);
    vertex(x - w/2, y - ht + detailHt, z);
    vertex(x - w/2 + detailW, y - ht + detailHt, z);
    // left foot
    vertex(x - w/2 + detailW,  y + stepL, z);
    curveVertex(x - w/2 + detailW, y + stepL, z);
    curveVertex(x - w/2 + detailW, y + stepL, z);
    curveVertex(x - w/2 + detailW - shoeBulge,  y + detailHt/2 + stepL, z);
    curveVertex(x - w/2,  y + detailHt + stepL, z);
    curveVertex(x - w/2,  y + detailHt + stepL, z);
    vertex(x - w/2 + footWidth,  y + detailHt + stepL*.9f, z);
    // end left foot
    vertex(x - w/2 + footWidth + legGap/2,  y - ht + detailHt, z);
    vertex(x - w/2 + footWidth + legGap/2,  y - ht + detailHt, z);
    // right foot
    vertex(x - w/2 + footWidth + legGap,  y + detailHt + stepR*.9f, z);
    vertex(x + w/2,  y + detailHt + stepR, z);
    curveVertex(x + w/2,  y + detailHt + stepR, z);
    curveVertex(x + w/2,  y + detailHt + stepR, z);
    curveVertex(x + w/2 - detailW + shoeBulge,  y + detailHt/2 + stepR, z);
    curveVertex(x + w/2 - detailW,  y + stepR, z);
    vertex(x + w/2 - detailW,  y + stepR, z);
    // end right foot
    vertex(x + w/2 - detailW,  y - ht + detailHt, z);
    vertex(x + w/2,  y - ht + detailHt, z);
    vertex(x + w/2,  y - ht, z);
    endShape(CLOSE);
  }

  // Set advanced property values
  public void setDetails(float detailW, float detailHt, float shoeBulge, float legGap) {
    this.detailW = detailW;
    this.detailHt = detailHt;
    this.shoeBulge = shoeBulge;
    this.legGap = legGap;
  }

  // Make the legs step
  public void step(float stepRate) {
    this.stepRate = stepRate;
    spring = ht/2.0f;
    stepL = sin(theta)*spring;
    stepR = cos(theta)*spring;
    theta += radians(stepRate);
  }
  
  // Alternative overloaded step method
  public void step() {
    spring = ht/2.0f;
    stepL = sin(theta)*spring;
    stepR = cos(theta)*spring;
    theta += radians(stepRate);
  }


  // Moves legs along x, y, z axes
  public void move() {
    // Move legs along y-axis
    y = stepR*damping;

    // Move legs along x-axis and
    // check for collision against frame edge
    x += speedX;
    if (screenX(x, y, z) > width) {
      speedX *= -1;
    }
    if (screenX(x, y, z) < 0) {
      speedX *= -1;
    }

    // Move legs along z-axis based on speed of stepping 
    // and check for collision against extremes
    speedZ = (stepRate*velocity);
    z += speedZ;
    if (z > 400) {
      z = 400;
      velocity *= -1;
    }
    if (z < -100) {
      z = -100;
      velocity *= -1;
    }
  }
  
  public void setDynamics(float speedX, float spring, float damping) {
    this.speedX = speedX;
    this.spring = spring;
    this.damping = damping;
  }
}













  static public void main(String args[]) {
    PApplet.main(new String[] { "--present", "--bgcolor=#666666", "--hide-stop", "RunAmuck" });
  }
}
