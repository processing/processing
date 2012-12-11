// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

class Mover {

  PVector location;
  PVector velocity;
  PVector acceleration;
  float mass;

  Mover(float m, float x, float y, float z) {
    mass = m;
    location = new PVector(x,y,z);
    velocity = new PVector(1,0);
    acceleration = new PVector(0,0);
  }

  void applyForce(PVector force) {
    PVector f = PVector.div(force,mass);
    acceleration.add(f);
  }

  void update() {
    velocity.add(acceleration);
    location.add(velocity);
    acceleration.mult(0);
  }

  void display() {
    noStroke();
    fill(255);
    pushMatrix();
    translate(location.x,location.y,location.z);
    sphere(mass*8);
    popMatrix();
  }

  void checkEdges() {

    if (location.x > width) {
      location.x = 0;
    } 
    else if (location.x < 0) {
      location.x = width;
    }

    if (location.y > height) {
      velocity.y *= -1;
      location.y = height;
    }
  }
}


