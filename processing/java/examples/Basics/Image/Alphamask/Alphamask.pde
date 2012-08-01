/**
 * Alpha Mask. 
 * 
 * Loads a "mask" for an image to specify the transparency 
 * in different parts of the image. The two images are blended
 * together using the mask() method of PImage. 
 */
 
PImage img;
PImage maskImg;

void setup() {
  size(200, 200);
  img = loadImage("test.jpg");
  maskImg = loadImage("mask.jpg");
  img.mask(maskImg);
  imageMode(CENTER);
}

void draw() {
  background(map(mouseX+mouseY, 0, width+height, 0, 255));
  image(img, width/2, height/2);
  image(img, mouseX, mouseY);
}
