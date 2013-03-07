// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

class Vehicle {

  PVector location;
  PVector velocity;
  PVector acceleration;
  float r;

  float maxspeed;
  float maxforce;
  
  Vehicle(float x, float y) {
    acceleration = new PVector(0, 0);
    velocity = new PVector(1,0);
    velocity.mult(5);
    location = new PVector(x, y);
    r = 3;
    maxspeed = 3;
    maxforce = 0.15;
  }

  void run() {
    update();
    display();
  }

  // Method to update location
  void update() {
    // Update velocity
    velocity.add(acceleration);
    // Limit speed
    velocity.limit(maxspeed);
    location.add(velocity);
    // Reset accelertion to 0 each cycle
    acceleration.mult(0);
  }

  void boundaries() {

    PVector desired = null;
    
    // Predict location 25 (arbitrary choice) frames ahead
    PVector predict = velocity.get();
    predict.mult(25);
    PVector futureLocation = PVector.add(location, predict);
    float distance = PVector.dist(futureLocation,circleLocation);
    
    if (distance > circleRadius) {
      PVector toCenter = PVector.sub(circleLocation,location);
      toCenter.normalize();
      toCenter.mult(velocity.mag());
      desired = PVector.add(velocity,toCenter);
      desired.normalize();
      desired.mult(maxspeed);
    }

    if (desired != null) {
      PVector steer = PVector.sub(desired, velocity);
      steer.limit(maxforce);
      applyForce(steer);
    }
    
    fill(255,0,0);
    ellipse(futureLocation.x,futureLocation.y,4,4);
    
  }  

  void applyForce(PVector force) {
    // We could add mass here if we want A = F / M
    acceleration.add(force);
  }


  void display() {
    // Draw a triangle rotated in the direction of velocity
    float theta = velocity.heading2D() + radians(90);
    fill(175);
    stroke(0);
    pushMatrix();
    translate(location.x, location.y);
    rotate(theta);
    beginShape(TRIANGLES);
    vertex(0, -r*2);
    vertex(-r, r*2);
    vertex(r, r*2);
    endShape();
    popMatrix();
  }
}

