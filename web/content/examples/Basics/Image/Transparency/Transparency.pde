/**
 * Transparency. 
 * 
 * Move the pointer left and right across the image to change
 * its position. This program overlays one image over another 
 * by modifying the alpha value of the image with the tint() function. 
 */

PImage a, b;
float offset;

void setup() {
  size(200, 200);
  a = loadImage("construct.jpg");  // Load an image into the program 
  b = loadImage("wash.jpg");   // Load an image into the program 
  frameRate(60);
}

void draw() { 
  image(a, 0, 0);
  float offsetTarget = map(mouseX, 0, width, -b.width/2 - width/2, 0);
  offset += (offsetTarget-offset)*0.05; 
  tint(255, 153);
  image(b, offset, 20);
}





