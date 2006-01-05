// Displaying
// by REAS <http://reas.com>

// Images can be displayed to the screen at their actual size
// or any other size. Only JPG and GIF images may be loaded.

// Created 2 November 2002

size(200, 200);
PImage a;  // Declare variable "a" of type PImage
a = loadImage("arch.jpg"); // Load the images into the program
image(a, 0, 0); // Displays the image from point (0,0)
image(a, width/2, 0, a.width/2, a.height/2);
