/**
 * Texture Quad. 
 * 
 * Load an image and draw it onto a quad. The texture() function sets
 * the texture image. The vertex() function maps the image to the geometry.
 */

// @pjs preload must be used to preload media if the program is 
// running with Processing.js
/* @pjs preload="berlin-1.jpg"; */ 

PImage img;

void setup() {
  size(640, 360, P3D);
  img = loadImage("berlin-1.jpg");
  noStroke();
}

void draw() {
  background(0);
  translate(width / 2, height / 2);
  rotateY(map(mouseX, 0, width, -PI, PI));
  rotateZ(PI/6);
  beginShape();
  texture(img);
  vertex(-100, -100, 0, 0, 0);
  vertex(100, -100, 0, 300, 0);
  vertex(100, 100, 0, 300, 300);
  vertex(-100, 100, 0, 0, 300);
  endShape();
}
