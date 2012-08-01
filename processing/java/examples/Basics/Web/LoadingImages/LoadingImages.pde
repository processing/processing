/**
 * Loading Images. 
 * 
 * Processing applications can only load images from the network
 * while running in the Processing environment. 
 * 
 * This example will not run in a web broswer and will only work when 
 * the computer is connected to the Internet. 
 */
 
size(200, 200);
PImage img1;
img1 = loadImage("http://processing.org/img/processing_cover.gif");
if (img != null) {
  image(img1, 0, 0);
}
