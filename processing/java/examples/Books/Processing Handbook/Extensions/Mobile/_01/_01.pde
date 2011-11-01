// The image file, named sprite.png in this example, must be located in the
// sketch data folder. From the Sketch menu, choose "Add File" to copy files into
// the sketch data folder.
PImage img = loadImage("sprite.png");
// The coordinates (0, 0) refer to the top-left corder of the screen
image(img, 0, 0);
// The following coordinate calculations will center the image in the screen
image(img, (width – img.width) / 2, (height – img.height) / 2);
// Finally, the next line will position the image in the bottom-right corner
image(img, width – img.width, height – img.height);