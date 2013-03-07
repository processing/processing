// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Simple Particle System

class Particle {
  PVector loc;
  PVector vel;
  PVector acc;
  float lifespan;

  // Another constructor (the one we are using here)
  Particle(PVector l) {
    // Boring example with constant acceleration
    acc = new PVector(0,0.05,0);
    vel = new PVector(random(-1,1),random(-1,0),0);
    vel.mult(2);
    loc = l.get();
    lifespan = 255;
  }

  void run() {
    update();
    render();
  }

  // Method to update location
  void update() {
    vel.add(acc);
    loc.add(vel);
    lifespan -= 2.0;
  }

  // Method to display
  void render() {
    imageMode(CENTER);
    tint(lifespan);
    image(img,loc.x,loc.y);
  }
  
  // Is the particle still useful?
  boolean isDead() {
    if (lifespan <= 0.0) {
      return true;
    } else {
      return false;
    }
  }
}


