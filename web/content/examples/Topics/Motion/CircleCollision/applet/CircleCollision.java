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

public class CircleCollision extends PApplet {

/**
 * Circle Collision with Swapping Velocities
 * by Ira Greenberg. 
 * 
 * Based on Keith Peter's Solution in
 * Foundation Actionscript Animation: Making Things Move!
 */

Ball[] balls =  { 
  new Ball(100, 400, 20), 
  new Ball(700, 400, 80) 
};

PVector[] vels = { 
  new PVector(2.15f, -1.35f), 
  new PVector(-1.65f, .42f) 
};

public void setup() {
  size(640, 360);
  smooth();
  noStroke();
}

public void draw() {
  background(51);
  fill(204);
  for (int i=0; i< 2; i++){
    balls[i].x += vels[i].x;
    balls[i].y += vels[i].y;
    ellipse(balls[i].x, balls[i].y, balls[i].r*2, balls[i].r*2);
    checkBoundaryCollision(balls[i], vels[i]);
  }
  checkObjectCollision(balls, vels);
}

public void checkObjectCollision(Ball[] b, PVector[] v){

  // get distances between the balls components
  PVector bVect = new PVector();
  bVect.x = b[1].x - b[0].x;
  bVect.y = b[1].y - b[0].y;

  // calculate magnitude of the vector separating the balls
  float bVectMag = sqrt(bVect.x * bVect.x + bVect.y * bVect.y);
  if (bVectMag < b[0].r + b[1].r){
    // get angle of bVect
    float theta  = atan2(bVect.y, bVect.x);
    // precalculate trig values
    float sine = sin(theta);
    float cosine = cos(theta);

    /* bTemp will hold rotated ball positions. You 
     just need to worry about bTemp[1] position*/
    Ball[] bTemp = {  
      new Ball(), new Ball()          };
      
    /* b[1]'s position is relative to b[0]'s
     so you can use the vector between them (bVect) as the 
     reference point in the rotation expressions.
     bTemp[0].x and bTemp[0].y will initialize
     automatically to 0.0, which is what you want
     since b[1] will rotate around b[0] */
    bTemp[1].x  = cosine * bVect.x + sine * bVect.y;
    bTemp[1].y  = cosine * bVect.y - sine * bVect.x;

    // rotate Temporary velocities
    PVector[] vTemp = { 
      new PVector(), new PVector()         };
    vTemp[0].x  = cosine * v[0].x + sine * v[0].y;
    vTemp[0].y  = cosine * v[0].y - sine * v[0].x;
    vTemp[1].x  = cosine * v[1].x + sine * v[1].y;
    vTemp[1].y  = cosine * v[1].y - sine * v[1].x;

    /* Now that velocities are rotated, you can use 1D
     conservation of momentum equations to calculate 
     the final velocity along the x-axis. */
    PVector[] vFinal = {  
      new PVector(), new PVector()          };
    // final rotated velocity for b[0]
    vFinal[0].x = ((b[0].m - b[1].m) * vTemp[0].x + 2 * b[1].m * 
      vTemp[1].x) / (b[0].m + b[1].m);
    vFinal[0].y = vTemp[0].y;
    // final rotated velocity for b[0]
    vFinal[1].x = ((b[1].m - b[0].m) * vTemp[1].x + 2 * b[0].m * 
      vTemp[0].x) / (b[0].m + b[1].m);
    vFinal[1].y = vTemp[1].y;

    // hack to avoid clumping
    bTemp[0].x += vFinal[0].x;
    bTemp[1].x += vFinal[1].x;

    /* Rotate ball positions and velocities back
     Reverse signs in trig expressions to rotate 
     in the opposite direction */
    // rotate balls
    Ball[] bFinal = { 
      new Ball(), new Ball()         };
    bFinal[0].x = cosine * bTemp[0].x - sine * bTemp[0].y;
    bFinal[0].y = cosine * bTemp[0].y + sine * bTemp[0].x;
    bFinal[1].x = cosine * bTemp[1].x - sine * bTemp[1].y;
    bFinal[1].y = cosine * bTemp[1].y + sine * bTemp[1].x;

    // update balls to screen position
    b[1].x = b[0].x + bFinal[1].x;
    b[1].y = b[0].y + bFinal[1].y;
    b[0].x = b[0].x + bFinal[0].x;
    b[0].y = b[0].y + bFinal[0].y;

    // update velocities
    v[0].x = cosine * vFinal[0].x - sine * vFinal[0].y;
    v[0].y = cosine * vFinal[0].y + sine * vFinal[0].x;
    v[1].x = cosine * vFinal[1].x - sine * vFinal[1].y;
    v[1].y = cosine * vFinal[1].y + sine * vFinal[1].x;
  }
}

public void checkBoundaryCollision(Ball ball, PVector vel) {
  if (ball.x > width-ball.r) {
    ball.x = width-ball.r;
    vel.x *= -1;
  } 
  else if (ball.x < ball.r) {
    ball.x = ball.r;
    vel.x *= -1;
  } 
  else if (ball.y > height-ball.r) {
    ball.y = height-ball.r;
    vel.y *= -1;
  } 
  else if (ball.y < ball.r) {
    ball.y = ball.r;
    vel.y *= -1;
  }
}

class Ball{
  float x, y, r, m;

  // default constructor
  Ball() {
  }

  Ball(float x, float y, float r) {
    this.x = x;
    this.y = y;
    this.r = r;
    m = r*.1f;
  }
}



  static public void main(String args[]) {
    PApplet.main(new String[] { "CircleCollision" });
  }
}
