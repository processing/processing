// Daniel Shiffman
// The Nature of Code
// http://www.shiffman.net/

// Declare a Random number generator object
Random generator;

void setup() {
  size(800, 200);
  background(255);
  generator = new Random();   // Initialize generator
  smooth();
}

void draw() {

  // Get a gaussian random number w/ mean of 0 and standard deviation of 1.0
  float xloc = (float) generator.nextGaussian();

  float sd = 60;                // Define a standard deviation
  float mean = width/2;         // Define a mean value (middle of the screen along the x-axis)
  xloc = ( xloc * sd ) + mean;  // Scale the gaussian random number by standard deviation and mean

    noStroke();
  fill(0, 10);
  noStroke();
  ellipse(xloc, height/2, 16, 16);   // Draw an ellipse at our "normal" random location
  
}



