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

public class BezierEllipse extends PApplet {

/**
 * Bezier Ellipse  
 * By Ira Greenberg 
 * 
 * Generates an ellipse using bezier() and
 * trig functions. Approximately every 1/2 
 * second a new ellipse is plotted using 
 * random values for control/anchor points.
 */

// arrays to hold ellipse coordinate data
float[] px, py, cx, cy, cx2, cy2;

// global variable-points in ellipse
int pts = 4;

int controlPtCol = 0xff222222;
int anchorPtCol = 0xffBBBBBB;

public void setup(){
  size(200, 200);
  smooth();
  setEllipse(pts, 65, 65);
  frameRate(1);
}

public void draw(){
  background(145);
  drawEllipse();
  setEllipse(PApplet.parseInt(random(3, 12)), random(-100, 150), random(-100, 150));
}

// draw ellipse with anchor/control points
public void drawEllipse(){
  strokeWeight(1.125f);
  stroke(255);
  noFill();
  // create ellipse
  for (int i=0; i<pts; i++){
    if (i==pts-1) {
      bezier(px[i], py[i], cx[i], cy[i], cx2[i], cy2[i],  px[0], py[0]);
    }
    else{
      bezier(px[i], py[i], cx[i], cy[i], cx2[i], cy2[i],  px[i+1], py[i+1]);
    }
  }
  strokeWeight(.75f);
  stroke(0);
  rectMode(CENTER);

  // control handles and tangent lines
  for ( int i=0; i< pts; i++){
    if (i==pts-1){  // last loop iteration-close path
      line(px[0], py[0], cx2[i], cy2[i]);
    }
    if (i>0){
      line(px[i], py[i], cx2[i-1], cy2[i-1]);
    }
    line(px[i], py[i], cx[i], cy[i]);
  }

  for ( int i=0; i< pts; i++){
    fill(controlPtCol);
    noStroke();
    //control handles
    ellipse(cx[i], cy[i], 4, 4);
    ellipse(cx2[i], cy2[i], 4, 4);

    fill(anchorPtCol);
    stroke(0);
    //anchor points
    rect(px[i], py[i], 5, 5);
  }
}

// fill up arrays with ellipse coordinate data
public void setEllipse(int points, float radius, float controlRadius){
  pts = points;
  px = new float[points];
  py = new float[points];
  cx = new float[points];
  cy = new float[points];
  cx2 = new float[points];
  cy2 = new float[points];
  float angle = 360.0f/points;
  float controlAngle1 = angle/3.0f;
  float controlAngle2 = controlAngle1*2.0f;
  for ( int i=0; i<points; i++){
    px[i] = width/2+cos(radians(angle))*radius;
    py[i] = height/2+sin(radians(angle))*radius;
    cx[i] = width/2+cos(radians(angle+controlAngle1))* 
      controlRadius/cos(radians(controlAngle1));
    cy[i] = height/2+sin(radians(angle+controlAngle1))* 
      controlRadius/cos(radians(controlAngle1));
    cx2[i] = width/2+cos(radians(angle+controlAngle2))* 
      controlRadius/cos(radians(controlAngle1));
    cy2[i] = height/2+sin(radians(angle+controlAngle2))* 
      controlRadius/cos(radians(controlAngle1));

    //increment angle so trig functions keep chugging along
    angle+=360.0f/points;
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "BezierEllipse" });
  }
}
