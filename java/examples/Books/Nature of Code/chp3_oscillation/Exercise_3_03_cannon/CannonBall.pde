// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

class CannonBall { 
  // All of our regular motion stuff
  PVector location;
  PVector velocity;
  PVector acceleration;

  // Size
  float r = 8;
  
  float topspeed = 10;

  CannonBall(float x, float y) {
    location = new PVector(x,y);
    velocity = new PVector();
    acceleration = new PVector();
  } 

  // Standard Euler integration
  void update() { 
    velocity.add(acceleration);
    velocity.limit(topspeed);
    location.add(velocity);
    acceleration.mult(0);
  }

  void applyForce(PVector force) {
    acceleration.add(force);
  }

  
  void display() { 
    stroke(0);
    strokeWeight(2);
    pushMatrix();
    translate(location.x,location.y);
    ellipse(0,0,r*2,r*2);
    popMatrix();
  }
}

