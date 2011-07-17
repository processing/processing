/**
 * Loading Images. 
 * 
 * Processing applications can only load images from the network
 * while running in the Processing environment. 
 * 
 * This example will not run in a web broswer and will only work when 
 * the computer is connected to the Internet. 
 */

PImage img;

void setup() {
  size(640, 360);
  img = loadImage("http://processing.org/img/processing_cover.gif");
  noLoop();
}

void draw() {
  background(0);
  if (img != null) {
    image(img, 0, 0);
    image(img, 0, img.height);
    image(img, 0, img.height * 2);
  }
}

