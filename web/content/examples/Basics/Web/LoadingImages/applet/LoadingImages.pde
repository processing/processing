/**
 * Loading Images. 
 * 
 * Loading a recent image from the US National Weather Service. 
 * Notice the date in the upper left corner of the image. 
 * Processing applications can only load images from the network
 * while running in the Processing environment. This example will 
 * not run in a web broswer and will only work when the computer
 * is connected to the Internet. 
 */
 
size(200, 200);
PImage img1;
img1 = loadImage("http://www.processing.org/img/processing_beta_cover.gif");
image(img1, 0, 45);

