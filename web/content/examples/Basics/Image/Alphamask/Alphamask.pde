/**
 * Alpha Mask. 
 * 
 * Loads a "mask" for an image to specify the transparency 
 * in different parts of the image. The two images are blended
 * together using the mask() method of PImage. 
 */
 
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
