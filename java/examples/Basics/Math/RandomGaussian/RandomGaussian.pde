/**
 * Random Gaussian. 
 * 
 * This sketch draws ellipses with x and y locations tied to a gaussian distribution of random numbers.
 */

void setup() {
  size(640, 360);
  background(0);
}

void draw() {

  // Get a gaussian random number w/ mean of 0 and standard deviation of 1.0
  float val = randomGaussian();

  float sd = 60;                  // Define a standard deviation
  float mean = width/2;           // Define a mean value (middle of the screen along the x-axis)
  float x = ( val * sd ) + mean;  // Scale the gaussian random number by standard deviation and mean

  noStroke();
  fill(255, 10);
  noStroke();
  ellipse(x, height/2, 32, 32);   // Draw an ellipse at our "normal" random location
}



