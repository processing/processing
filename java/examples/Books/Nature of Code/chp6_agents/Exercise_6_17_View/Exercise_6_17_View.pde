// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Exercise 6.17: Implement Flake's "View" rule

// This answer doesn't implement the rule, but rather demonstrates how a boid can 
// detect what is "in front" of it based on peripheral vision

Flock flock;

void setup() {
  size(640,360);
  flock = new Flock();
  // Add an initial set of boids into the system
  for (int i = 0; i < 25; i++) {
    Boid b = new Boid(width/2+random(0,75),height/2+random(0,75));
    flock.addBoid(b);
  }
}

void draw() {
  background(255);
  
  flock.run();
}

// Add a new boid into the System
void mouseDragged() {
  flock.addBoid(new Boid(mouseX,mouseY));
}
