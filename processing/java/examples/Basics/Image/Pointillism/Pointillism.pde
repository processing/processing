/**
 * Pointillism
 * by Daniel Shiffman. 
 * 
 * Mouse horizontal location controls size of dots. 
 * Creates a simple pointillist effect using ellipses colored
 * according to pixels in an image. 
 * 
 * Updated 27 February 2010.
 */
 
PImage img;

int smallPoint = 2;
int largePoint;
int top, left;

void setup() {
  size(200, 200);
  img = loadImage("eames.jpg");
  //img = loadImage("sunflower.jpg");  // an alternative image
  noStroke();
  background(255);
  smooth();
  largePoint = min(width, height) / 10;
  // center the image on the screen
  left = (width - img.width) / 2;
  top = (height - img.height) / 2;
}

void draw() { 
  float pointillize = map(mouseX, 0, width, smallPoint, largePoint);
  int x = int(random(img.width));
  int y = int(random(img.height));
  color pix = img.get(x, y);
  fill(pix, 128);
  ellipse(left + x, top + y, pointillize, pointillize);
}
