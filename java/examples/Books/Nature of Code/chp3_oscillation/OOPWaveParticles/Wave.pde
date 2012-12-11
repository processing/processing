// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

class Wave {
  
  int xspacing = 8; // How far apart should each horizontal location be spaced
  int w;            // Width of entire wave
  
  PVector origin;          // Where does the wave's first point start
  float theta = 0.0;       // Start angle at 0
  float amplitude;         // Height of wave
  float period;            // How many pixels before the wave repeats
  float dx;                // Value for incrementing X, to be calculated as a function of period and xspacing
  //float[] yvalues;         // Using an array to store height values for the wave (not entirely necessary)
  Particle[] particles;

  Wave(PVector o, int w_, float a, float p) {
    origin = o.get();
    w = w_;
    period = p;
    amplitude = a;
    dx = (TWO_PI / period) * xspacing;
    particles = new Particle[w/xspacing];
    for (int i = 0; i < particles.length; i++) {
      particles[i] = new Particle(); 
    }
  }


  void calculate() {
    // Increment theta (try different values for 'angular velocity' here
    theta += 0.02;

    // For every x value, calculate a y value with sine function
    float x = theta;
    for (int i = 0; i < particles.length; i++) {
      particles[i].setLocation(origin.x+i*xspacing,origin.y+sin(x)*amplitude);
      x+=dx;
    }
  }
  
  void manipulate() {
    // Loop through the array of particles and check stuff regarding the mouse
     
  }

  void display() {
    
    // A simple way to draw the wave with an ellipse at each location
    for (int i = 0; i < particles.length; i++) {
      particles[i].display();
    }
  }
}

