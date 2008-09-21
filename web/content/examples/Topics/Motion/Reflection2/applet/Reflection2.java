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

public class Reflection2 extends PApplet {

/**
 * Non-orthogonal Collision with Multiple Ground Segments 
 * by Ira Greenberg. 
 * 
 * Based on Keith Peter's Solution in
 * Foundation Actionscript Animation: Making Things Move!
 */

Orb orb;
Vect2D velocity;
float gravity = .05f, damping = 0.8f;
int segments = 40;
Ground[] ground = new Ground[segments];
float[] peakHeights = new float[segments+1];

public void setup(){
  size(640, 200);
  smooth();
  orb = new Orb(50, 50, 3);
  velocity = new Vect2D(.5f, 0);

  // Calculate ground peak heights 
  for (int i=0; i<peakHeights.length; i++){
    peakHeights[i] = random(height-40, height-30);
  }

  /* Float value required for segment width (segs)
   calculations so the ground spans the entire 
   display window, regardless of segment number. */
  float segs = segments;
  for (int i=0; i<segments; i++){
    ground[i]  = new Ground(width/segs*i, peakHeights[i],
    width/segs*(i+1), peakHeights[i+1]);
  }
}


public void draw(){
  // Background
  noStroke();
  fill(0, 15);
  rect(0, 0, width, height);

  // Move orb
  orb.x += velocity.vx;
  velocity.vy += gravity;
  orb.y += velocity.vy;

  // Draw ground
  fill(127);
  beginShape();
  for (int i=0; i<segments; i++){
    vertex(ground[i].x1, ground[i].y1);
    vertex(ground[i].x2, ground[i].y2);
  }
  vertex(ground[segments-1].x2, height);
  vertex(ground[0].x1, height);
  endShape(CLOSE);

  // Draw orb
  noStroke();
  fill(200);
  ellipse(orb.x, orb.y, orb.r*2, orb.r*2);

  // Collision detection
  checkWallCollision();
  for (int i=0; i<segments; i++){
    checkGroundCollision(ground[i]);
  }
}


public void checkWallCollision(){
  if (orb.x > width-orb.r){
    orb.x = width-orb.r;
    velocity.vx *= -1;
    velocity.vx *= damping;
  } 
  else if (orb.x < orb.r){
    orb.x = orb.r;
    velocity.vx *= -1;
    velocity.vx *= damping;
  }
}


public void checkGroundCollision(Ground groundSegment) {

  // Get difference between orb and ground
  float deltaX = orb.x - groundSegment.x;
  float deltaY = orb.y - groundSegment.y;

  // Precalculate trig values
  float cosine = cos(groundSegment.rot);
  float sine = sin(groundSegment.rot);

  /* Rotate ground and velocity to allow 
   orthogonal collision calculations */
  float groundXTemp = cosine * deltaX + sine * deltaY;
  float groundYTemp = cosine * deltaY - sine * deltaX;
  float velocityXTemp = cosine * velocity.vx + sine * velocity.vy;
  float velocityYTemp = cosine * velocity.vy - sine * velocity.vx;

  /* Ground collision - check for surface 
   collision and also that orb is within 
   left/rights bounds of ground segment */
  if (groundYTemp > -orb.r &&
    orb.x > groundSegment.x1 &&
    orb.x < groundSegment.x2 ){
    // keep orb from going into ground
    groundYTemp = -orb.r;
    // bounce and slow down orb
    velocityYTemp *= -1.0f;
    velocityYTemp *= damping;
  }

  // Reset ground, velocity and orb
  deltaX = cosine * groundXTemp - sine * groundYTemp;
  deltaY = cosine * groundYTemp + sine * groundXTemp;
  velocity.vx = cosine * velocityXTemp - sine * velocityYTemp;
  velocity.vy = cosine * velocityYTemp + sine * velocityXTemp;
  orb.x = groundSegment.x + deltaX;
  orb.y = groundSegment.y + deltaY;
}


class Ground {
  float x1, y1, x2, y2;  
  float x, y, len, rot;

  // Default constructor
  Ground(){
  }

  // Constructor
  Ground(float x1, float y1, float x2, float y2) {
    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x2;
    this.y2 = y2;
    x = (x1+x2)/2;
    y = (y1+y2)/2;
    len = dist(x1, y1, x2, y2);
    rot = atan2((y2-y1), (x2-x1));
  }
}


class Orb{
  float x, y, r;

  // Default constructor
  Orb() {
  }

  Orb(float x, float y, float r) {
    this.x = x;
    this.y = y;
    this.r = r;
  }
}

class Vect2D{
  float vx, vy;

  // Default constructor
  Vect2D() {
  }

  Vect2D(float vx, float vy) {
    this.vx = vx;
    this.vy = vy;
  }
}


  static public void main(String args[]) {
    PApplet.main(new String[] { "Reflection2" });
  }
}
