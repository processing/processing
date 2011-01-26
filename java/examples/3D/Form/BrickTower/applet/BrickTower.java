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

public class BrickTower extends PApplet {

/**
 * Brick Tower
 * by Ira Greenberg. 
 * 
 * 3D castle tower constructed out of individual bricks.
 * Uses the Point3D and Cube classes. 
 */


float bricksPerLayer = 16.0f;
float brickLayers = 18.0f;
Cube brick;
float brickWidth = 60, brickHeight = 25, brickDepth = 25;
float radius = 175.0f;
float angle = 0;

public void setup(){
  size(640, 360, P3D);
  brick = new Cube(brickWidth, brickHeight, brickDepth);
}

public void draw(){
  background(0);
  float tempX = 0, tempY = 0, tempZ = 0;
  fill(182, 62, 29);
  noStroke();
  // Add basic light setup
  lights();
  translate(width/2, height*1.2f, -380);
  // Tip tower to see inside
  rotateX(radians(-45));
  // Slowly rotate tower
  rotateY(frameCount * PI/600);
  for (int i = 0; i < brickLayers; i++){
    // Increment rows
    tempY-=brickHeight;
    // Alternate brick seams
    angle = 360.0f / bricksPerLayer * i/2;
    for (int j = 0; j < bricksPerLayer; j++){
      tempZ = cos(radians(angle))*radius;
      tempX = sin(radians(angle))*radius;
      pushMatrix();
      translate(tempX, tempY, tempZ);
      rotateY(radians(angle));
      // Add crenelation
      if (i==brickLayers-1){
        if (j%2 == 0){
          brick.create();
        }
      }
      // Create main tower
      else {
        brick.create();
      }
      popMatrix();
      angle += 360.0f/bricksPerLayer;
    }
  }
}


class Cube {
  
  Point3D[] vertices = new Point3D[24];
  float w, h, d;

  Cube(){ }

  Cube(float w, float h, float d){
    this.w = w;
    this.h = h;
    this.d = d;

    // Cube composed of 6 quads
    // Front
    vertices[0] = new Point3D(-w/2,-h/2,d/2);
    vertices[1] = new Point3D(w/2,-h/2,d/2);
    vertices[2] = new Point3D(w/2,h/2,d/2);
    vertices[3] = new Point3D(-w/2,h/2,d/2);
    
    // Left
    vertices[4] = new Point3D(-w/2,-h/2,d/2);
    vertices[5] = new Point3D(-w/2,-h/2,-d/2);
    vertices[6] = new Point3D(-w/2,h/2,-d/2);
    vertices[7] = new Point3D(-w/2,h/2,d/2);
    
    // Right
    vertices[8] = new Point3D(w/2,-h/2,d/2);
    vertices[9] = new Point3D(w/2,-h/2,-d/2);
    vertices[10] = new Point3D(w/2,h/2,-d/2);
    vertices[11] = new Point3D(w/2,h/2,d/2);
    
    // Back
    vertices[12] = new Point3D(-w/2,-h/2,-d/2);  
    vertices[13] = new Point3D(w/2,-h/2,-d/2);
    vertices[14] = new Point3D(w/2,h/2,-d/2);
    vertices[15] = new Point3D(-w/2,h/2,-d/2);
    
    // Top
    vertices[16] = new Point3D(-w/2,-h/2,d/2);
    vertices[17] = new Point3D(-w/2,-h/2,-d/2);
    vertices[18] = new Point3D(w/2,-h/2,-d/2);
    vertices[19] = new Point3D(w/2,-h/2,d/2);
    
    // Bottom
    vertices[20] = new Point3D(-w/2,h/2,d/2);
    vertices[21] = new Point3D(-w/2,h/2,-d/2);
    vertices[22] = new Point3D(w/2,h/2,-d/2);
    vertices[23] = new Point3D(w/2,h/2,d/2);
  }

  public void create(){
    for (int i=0; i<6; i++){
      beginShape(QUADS);
      for (int j = 0; j < 4; j++){
        vertex(vertices[j+4*i].x, vertices[j+4*i].y, vertices[j+4*i].z);
      }
      endShape();
    }
  }
} 
class Point3D {
  float x, y, z;

  Point3D(){ }

  Point3D(float x, float y, float z){
    this.x = x;
    this.y = y;
    this.z = z;
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "BrickTower" });
  }
}
