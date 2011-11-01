/**
 * Loading Images. 
 * 
 * Loads an image from over the network. Be sure to have INTERNET
 * permission enabled, otherwise img will always return null.
 */
 
size(200, 200);
PImage img1;
img1 = loadImage("http://processing.org/img/processing_cover.gif");
if (img != null) {
  image(img1, 0, 0);
}
