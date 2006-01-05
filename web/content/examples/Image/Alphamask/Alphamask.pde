// Alpha Mask
// by toxi <http://www.toxi.co.uk>

// Loads a "mask" for an image which specifies its transparency 
// in different parts of the image. The two images are blended
// together using the mask() method of PImage.

// Created 29 April 2003
// Updated 7 July 2004

PImage img;
PImage maskImg;

void setup() 
{
  size(200,200);
  img = loadImage("test.jpg");
  maskImg = loadImage("mask.jpg");
  img.mask(maskImg);
}

void draw() 
{
  background((mouseX+mouseY)/1.5);
  image(img, 50, 50);
  image(img, mouseX-50, mouseY-50);
}
