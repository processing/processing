// Matrix
// by Mixel

// Simple example utilizing the OpenGL Library for Processing. 

// Created 15 April 2005


import processing.opengl.*;

void setup() 
{
  size(800, 600, OPENGL);
  noStroke();
  fill(0, 102, 153, 26);
}

void draw() 
{
  float x = mouseX - width/2;
  background(255);
  translate(width/2, height/2);
  rotateZ(radians(mouseY/10));
  for(int i=-height/2; i<height/2; i+=10) {
    for(int j=-width/2; j<width/2; j+=10) {
      beginShape(QUADS);
      vertex(x+j,   i,    0);
      vertex(x+j, i+5,    0);
      vertex(j-x, i+5, -400);
      vertex(j-x,   i, -400);
      endShape();
    }
  }
}

