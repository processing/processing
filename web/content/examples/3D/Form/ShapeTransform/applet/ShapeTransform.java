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

public class ShapeTransform extends PApplet {

/**
 * Shape Transform
 * by Ira Greenberg.  
 * 
 * Illustrates the geometric relationship 
 * between Cube, Pyramid, Cone and 
 * Cylinder 3D primitives.
 * 
 * Instructions:
 * Up Arrow - increases points
 * Down Arrow - decreases points
 * 'p' key toggles between cube/pyramid
 */

int pts = 7; 
float angle = 0;
float radius = 89;
float cylinderLength = 85;

Point3D vertices[][];
boolean isPyramid = false;

public void setup(){
  size(640, 360, P3D);
  noStroke();
  fill(204);
}

public void draw(){
  background(102);
  lights();

  translate(width/2, height/2, 0);
  rotateX(frameCount * 0.006f);
  rotateY(frameCount * 0.006f);
  rotateZ(frameCount * 0.006f);

  // initialize point arrays
  vertices = new Point3D[2][pts+1];

  // fill arrays
  for (int i = 0; i < 2; i++){
    angle = 0;
    for(int j = 0; j <= pts; j++){
      vertices[i][j] = new Point3D();
      if (isPyramid){
        if (i==1){
          vertices[i][j].x = 0;
          vertices[i][j].y = 0;
        }
        else {
          vertices[i][j].x = cos(radians(angle))*radius;
          vertices[i][j].y = sin(radians(angle))*radius;
        }
      } 
      else {
        vertices[i][j].x = cos(radians(angle))*radius;
        vertices[i][j].y = sin(radians(angle))*radius;
      }
      vertices[i][j].z = cylinderLength; 
      // the .0 after the 360 is critical
      angle += 360.0f/pts;
    }
    cylinderLength*=-1;
  }

  // draw cylinder tube
  beginShape(QUAD_STRIP);
  for (int j = 0; j <= pts; j++){
    vertex(vertices[0][j].x, vertices[0][j].y, vertices[0][j].z);
    vertex(vertices[1][j].x, vertices[1][j].y, vertices[1][j].z);
  }
  endShape();

  //draw cylinder ends
  for (int i = 0; i < 2; i++){
    beginShape();
    for (int j = 0; j < pts; j++){
      vertex(vertices[i][j].x, vertices[i][j].y, vertices[i][j].z);
    }
    endShape(CLOSE);
  }
}

/*
 up/down arrow keys control
 polygon detail.
 */
public void keyPressed(){
  if(key == CODED) { 
    // pts
    if (keyCode == UP) { 
      if (pts<90){
        pts++;
      } 
    } 
    else if (keyCode == DOWN) { 
      if (pts > 4){
        pts--;
      }
    } 
  }
  if (key == 'p'){
    if (isPyramid){
      isPyramid=false;
    } 
    else {
      isPyramid=true;
    }
  }
}



class Point3D{
  float x, y, z;

  // constructors
  Point3D(){
  }

  Point3D(float x, float y, float z){
    this.x = x;
    this.y = y;
    this.z = z;
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "ShapeTransform" });
  }
}
