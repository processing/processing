/**
 * Load and Display 
 * 
 * Images can be loaded and displayed to the screen at their actual size
 * or any other size. 
 */
 
PImage a;  // Declare variable "a" of type PImage

void setup() {
  size(200, 200);
  // The file "jelly.jpg" must be in the data folder
  // of the current sketch to load successfully
  a = loadImage("jelly.jpg");  // Load the image into the program  
  noLoop();  // Makes draw() only run once
}

void draw() {
  // Displays the image at its actual size at point (0,0)
  image(a, 0, 0); 
  // Displays the image at point (100, 0) at half of its size
  image(a, 100, 0, a.width/2, a.height/2);
}
