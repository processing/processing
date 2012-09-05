/**
 * Texture Triangle. 
 * 
 * Using a rectangular image to map a texture onto a triangle.
 */

// The next line is needed if running in JavaScript Mode with Processing.js
/* @pjs preload="berlin-1.jpg"; */ 

PImage img;

void setup() {
  size(640, 360, P3D);
  orientation(LANDSCAPE);
  img = loadImage("berlin-1.jpg");
  noStroke();
}

void draw() {
  background(0);
  translate(width/2, height/2, 0);
  rotateY(map(mouseX, 0, width, -PI, PI));
  beginShape();
  texture(img);
  vertex(-100, -100, 0, 0, 0);
  vertex(100, -40, 0, 300, 120);
  vertex(0, 100, 0, 200, 400);
  endShape();
}
