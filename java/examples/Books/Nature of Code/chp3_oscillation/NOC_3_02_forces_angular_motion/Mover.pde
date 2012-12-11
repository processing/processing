// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

class Mover {

  PVector location;
  PVector velocity;
  PVector acceleration;
  float mass;

  float angle = 0;
  float aVelocity = 0;
  float aAcceleration = 0;

  Mover(float m, float x, float y) {
    mass = m;
    location = new PVector(x,y);
    velocity = new PVector(random(-1,1),random(-1,1));
    acceleration = new PVector(0,0);
  }

  void applyForce(PVector force) {
    PVector f = PVector.div(force,mass);
    acceleration.add(f);
  }

  void update() {

    velocity.add(acceleration);
    location.add(velocity);

    aAcceleration = acceleration.x / 10.0;
    aVelocity += aAcceleration;
    aVelocity = constrain(aVelocity,-0.1,0.1);
    angle += aVelocity;

    acceleration.mult(0);
  }

  void display() {
    stroke(0);
    fill(175,200);
    rectMode(CENTER);
    pushMatrix();
    translate(location.x,location.y);
    rotate(angle);
    rect(0,0,mass*16,mass*16);
    popMatrix();
  }

}

