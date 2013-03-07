// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Simple Particle System

class Particle {
  PVector location;
  PVector velocity;
  PVector acceleration;
  float lifespan;
  
  float r;
  
  Particle(float x, float y, float r_) {
    acceleration = new PVector(0,0.01);
    velocity = PVector.random2D();
    velocity.mult(0.5);
    location = new PVector(x,y);
    lifespan = 255.0;
    r = r_;
  }

  void run() {
    update();
    display();
  }

  // Method to update location
  void update() {
    velocity.add(acceleration);
    location.add(velocity);
    lifespan -= 2.0;
  }

  // Method to display
  void display() {
    stroke(0);
    fill(0);
    rectMode(CENTER);
    rect(location.x,location.y,r,r);
  }
  
  // Is the particle still useful?
  boolean isDead() {
    if (lifespan < 0.0) {
      return true;
    } else {
      return false;
    }
  }
}


