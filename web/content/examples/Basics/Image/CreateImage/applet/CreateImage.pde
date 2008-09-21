/**
 * Create Image. 
 * 
 * The createImage() function provides a fresh buffer of pixels to play with.
 * This example creates an image gradient.
 */

PImage img;

void setup() 
{
  size(200, 200);  
  img = createImage(120, 120, ARGB);
  for(int i=0; i < img.pixels.length; i++) {
    img.pixels[i] = color(0, 90, 102, i%img.width * 2); 
  }
}

void draw() 
{
  background(204);
  image(img, 33, 33);
  image(img, mouseX-60, mouseY-60);
}
