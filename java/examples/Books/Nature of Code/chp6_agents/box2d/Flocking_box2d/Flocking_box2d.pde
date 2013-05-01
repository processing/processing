// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Flocking

// Demonstration of Craig Reynolds' "Flocking" behavior
// See: http://www.red3d.com/cwr/
// Rules: Cohesion, Separation, Alignment

// Click mouse to add boids into the system

import pbox2d.*;
import org.jbox2d.collision.shapes.*;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.*;

// A reference to our box2d world
PBox2D box2d;

Flock flock;

void setup() {
  size(640,360);
  // Initialize box2d physics and create the world
  box2d = new PBox2D(this);
  box2d.createWorld();
  // We are setting a custom gravity
  box2d.setGravity(0,0);
  
  flock = new Flock();
  // Add an initial set of boids into the system
  for (int i = 0; i < 50; i++) {
    flock.addBoid(new Boid(new PVector(random(width),random(height))));
  }
}

void draw() {
  // We must always step through time!
  box2d.step();

  background(255);
  flock.run();
}

void mousePressed() {
   flock.addBoid(new Boid(new PVector(mouseX,mouseY)));
}

void mouseDragged() {
   flock.addBoid(new Boid(new PVector(mouseX,mouseY)));
}


