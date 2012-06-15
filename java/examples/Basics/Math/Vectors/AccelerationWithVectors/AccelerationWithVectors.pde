/**
 * Acceleration with Vectors 
 * by Daniel Shiffman.  
 * 
 * Demonstration of the basics of motion with vector.
 * A "Mover" object stores location, velocity, and acceleration as vectors
 * The motion is controlled by affecting the acceleration (in this case towards the mouse)
 */

// A Mover object
Mover mover;

void setup() {
  size(640,360);
  smooth();
  mover = new Mover(); 
}

void draw() {
  background(0);
  
  // Update the location
  mover.update();
  // Display the Mover
  mover.display(); 
}

