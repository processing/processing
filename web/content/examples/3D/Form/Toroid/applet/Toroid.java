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

public class Toroid extends PApplet {

/**
 * Interactive Toroid
 * by Ira Greenberg. 
 * 
 * Illustrates the geometric relationship 
 * between Toroid, Sphere, and Helix
 * 3D primitives, as well as lathing
 * principal.
 * 
 * Instructions:
 * UP arrow key pts++;
 * DOWN arrow key pts--;
 * LEFT arrow key segments--; 
 * RIGHT arrow key segments++;
 * 'a' key toroid radius--; 
 * 's' key toroid radius++; 
 * 'z' key initial polygon radius--;
 * 'x' key initial polygon radius++;
 * 'w' key toggle wireframe/solid shading
 * 'h' key toggle sphere/helix
 */

// Point3D class required
int pts = 40; 
float angle = 0;
float radius = 40.0f;
// lathe segments
int segments = 60;
float latheAngle = 0;
float latheRadius = 100.0f;
//vertices
Point3D vertices[], vertices2[];
// for shaded or wireframe rendering 
boolean isWireFrame = false;
// for optional helix
boolean isHelix = false;
float helixOffset = 5.0f;

public void setup(){
  size(640, 360, P3D);
}

public void draw(){
  background(51);
  // basic lighting setup
  lights();
  // 2 rendering styles
  // wireframe or solid
  if (isWireFrame){
    stroke(255);
    noFill();
  } 
  else {
    noStroke();
    fill(204);
  }
  //center and spin toroid
  translate(width/2, height/2, -100);

  rotateX(frameCount*PI/150);
  rotateY(frameCount*PI/170);
  rotateZ(frameCount*PI/90);

  // initialize point arrays
  vertices = new Point3D[pts+1];
  vertices2 = new Point3D[pts+1];

  // fill arrays
  for(int i=0; i<=pts; i++){
    vertices[i] = new Point3D();
    vertices2[i] = new Point3D();
    vertices[i].x = latheRadius + sin(radians(angle))*radius;
    if (isHelix){
      vertices[i].z = cos(radians(angle))*radius-(helixOffset* 
        segments)/2;
    } 
    else{
      vertices[i].z = cos(radians(angle))*radius;
    }
    angle+=360.0f/pts;
  }

  // draw toroid
  latheAngle = 0;
  for(int i=0; i<=segments; i++){
    beginShape(QUAD_STRIP);
    for(int j=0; j<=pts; j++){
      if (i>0){
        vertex(vertices2[j].x, vertices2[j].y, vertices2[j].z);
      }
      vertices2[j].x = cos(radians(latheAngle))*vertices[j].x;
      vertices2[j].y = sin(radians(latheAngle))*vertices[j].x;
      vertices2[j].z = vertices[j].z;
      // optional helix offset
      if (isHelix){
        vertices[j].z+=helixOffset;
      } 
      vertex(vertices2[j].x, vertices2[j].y, vertices2[j].z);
    }
    // create extra rotation for helix
    if (isHelix){
      latheAngle+=720.0f/segments;
    } 
    else {
      latheAngle+=360.0f/segments;
    }
    endShape();
  }
}

/*
 left/right arrow keys control ellipse detail
 up/down arrow keys control segment detail.
 'a','s' keys control lathe radius
 'z','x' keys control ellipse radius
 'w' key toggles between wireframe and solid
 'h' key toggles between toroid and helix
 */
public void keyPressed(){
  if(key == CODED) { 
    // pts
    if (keyCode == UP) { 
      if (pts<40){
        pts++;
      } 
    } 
    else if (keyCode == DOWN) { 
      if (pts>3){
        pts--;
      }
    } 
    // extrusion length
    if (keyCode == LEFT) { 
      if (segments>3){
        segments--; 
      }
    } 
    else if (keyCode == RIGHT) { 
      if (segments<80){
        segments++; 
      }
    } 
  }
  // lathe radius
  if (key =='a'){
    if (latheRadius>0){
      latheRadius--; 
    }
  } 
  else if (key == 's'){
    latheRadius++; 
  }
  // ellipse radius
  if (key =='z'){
    if (radius>10){
      radius--;
    }
  } 
  else if (key == 'x'){
    radius++;
  }
  // wireframe
  if (key =='w'){
    if (isWireFrame){
      isWireFrame=false;
    } 
    else {
      isWireFrame=true;
    }
  }
  // helix
  if (key =='h'){
    if (isHelix){
      isHelix=false;
    } 
    else {
      isHelix=true;
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
    PApplet.main(new String[] { "Toroid" });
  }
}
