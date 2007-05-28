/**
 * Texture 1. 
 *
 * Load an image and draw it onto a quad. The texture() function sets
 * the texture image. The vertex() function maps the image to the geometry.
 *
 * Created 27 May 2007.
 *
 */

PImage img;

void setup() {
  size(200, 200, P3D);
  img = loadImage("berlin-1.jpg");
  noStroke();
}

void draw() {
  background(0);
  translate(width / 2, height / 2);
  rotateY(map(mouseX, 0, width, -PI, PI));
  beginShape();
  texture(img);
  vertex(-50, -50, 0, 0, 0);
  vertex(50, -50, 0, 400, 0);
  vertex(50, 50, 0, 400, 400);
  vertex(-50, 50, 0, 0, 400);
  endShape();
}
