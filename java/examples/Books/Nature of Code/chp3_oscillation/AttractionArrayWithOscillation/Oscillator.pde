// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Attraction Array with Oscillating objects around each thing

class Oscillator {
  
  // Because we are going to oscillate along the x and y axis we can use PVector for two angles, amplitudes, etc.!
  float theta;
  float amplitude;

  Oscillator(float r) {
    
    // Initialize randomly
    theta = 0;
    amplitude = r;

  }

  // Update theta and offset
  void update(float thetaVel) {
    theta += thetaVel;
  }

  // Display based on a location
  void display(PVector loc) {
    float x = map(cos(theta),-1,1,0,amplitude);
    
    stroke(0);
    fill(50);
    line(0,0,x,0);
    ellipse(x,0,8,8);
  }
}


