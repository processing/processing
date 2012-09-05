/**
 * Texture Cylinder. 
 * 
 * Load an image and draw it onto a cylinder and a quad. 
 */

// The next line is needed if running in JavaScript Mode with Processing.js
/* @pjs preload="berlin-1.jpg"; */ 

int tubeRes = 32;
float[] tubeX = new float[tubeRes];
float[] tubeY = new float[tubeRes];
PImage img;

void setup() {
  size(640, 360, P3D);
  orientation(LANDSCAPE);
  img = loadImage("berlin-1.jpg");
  float angle = 270.0 / tubeRes;
  for (int i = 0; i < tubeRes; i++) {
    tubeX[i] = cos(radians(i * angle));
    tubeY[i] = sin(radians(i * angle));
  }
  noStroke();
}

void draw() {
  background(0);
  translate(width / 2, height / 2);
  rotateX(map(mouseY, 0, height, -PI, PI));
  rotateY(map(mouseX, 0, width, -PI, PI));
  beginShape(QUAD_STRIP);
  texture(img);
  for (int i = 0; i < tubeRes; i++) {
    float x = tubeX[i] * 100;
    float z = tubeY[i] * 100;
    float u = img.width / tubeRes * i;
    vertex(x, -100, z, u, 0);
    vertex(x, 100, z, u, img.height);
  }
  endShape();
  beginShape(QUADS);
  texture(img);
  vertex(0, -100, 0, 0, 0);
  vertex(100, -100, 0, 100, 0);
  vertex(100, 100, 0, 100, 100);
  vertex(0, 100, 0, 0, 100);
  endShape();
}
