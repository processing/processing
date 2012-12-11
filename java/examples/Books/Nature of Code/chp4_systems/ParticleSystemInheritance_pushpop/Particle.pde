// Simple Particle System
// Daniel Shiffman <http://www.shiffman.net>

// A simple Particle class

class Particle {
  
  PVector location;
  PVector velocity;
  PVector acceleration;
  float lifespan;

  Particle(PVector l) {
    acceleration = new PVector(0,0.05);
    velocity = new PVector(random(-1,1),random(-2,0));
    location = l.get();
    lifespan = 255.0;
  }

  void run() {
    update();
    push();
    display();
    pop();
  }

  // Method to update location
  void update() {
    velocity.add(acceleration);
    location.add(velocity);
    lifespan -= 2.0;
  }


  void push() {
    pushMatrix();
  }
  
  void pop() {
    popMatrix();
  }

  // Method to display
  void display() {
    stroke(0,lifespan);
    fill(0,lifespan);
    translate(location.x,location.y);
    ellipse(0,0,8,8);
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


