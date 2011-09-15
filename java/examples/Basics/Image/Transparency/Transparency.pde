/**
 * Transparency. 
 * 
 * Move the pointer left and right across the image to change
 * its position. This program overlays one image over another 
 * by modifying the alpha value of the image with the tint() function. 
 */

// @pjs preload must be used to preload media if the program is 
// running with Processing.js
/* @pjs preload="moonwalk.jpg"; */ 

PImage img;
float offset = 0;
float easing = 0.05;

void setup() {
  size(640, 360);
  img = loadImage("moonwalk.jpg");  // Load an image into the program 
}

void draw() { 
  image(img, 0, 0);  // Display at full opacity
  float dx = (mouseX-img.width/2) - offset;
  offset += dx * easing; 
  tint(255, 126);  // Display at half opacity
  image(img, offset, 0);
}





