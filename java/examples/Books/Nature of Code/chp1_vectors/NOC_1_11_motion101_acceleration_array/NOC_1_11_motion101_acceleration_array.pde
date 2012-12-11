// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Demonstration of the basics of motion with vector.
// A "Mover" object stores location, velocity, and acceleration as vectors
// The motion is controlled by affecting the acceleration (in this case towards the mouse)

Mover[] movers = new Mover[20];

void setup() {
  size(800,200);
  for (int i = 0; i < movers.length; i++) {
    movers[i] = new Mover(); 
  }
}

void draw() {
  
  background(255);

  for (int i = 0; i < movers.length; i++) {
    movers[i].update();
    movers[i].display(); 
  }
}


