// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Chapter 3: Asteroids

class Spaceship { 
  // All of our regular motion stuff
  PVector location;
  PVector velocity;
  PVector acceleration;
  
  ParticleSystem ps;

  // Arbitrary damping to slow down ship
  float damping = 0.995;
  float topspeed = 6;

  // Variable for heading!
  float heading = 0;

  // Size
  float r = 16;

  // Are we thrusting (to color boosters)
  boolean thrusting = false;

  Spaceship() {
    location = new PVector(width/2,height/2);
    velocity = new PVector();
    acceleration = new PVector();
    
    ps = new ParticleSystem();
  } 

  // Standard Euler integration
  void update() { 
    velocity.add(acceleration);
    velocity.mult(damping);
    velocity.limit(topspeed);
    location.add(velocity);
    acceleration.mult(0);
    
    ps.run();
  }

  // Newton's law: F = M * A
  void applyForce(PVector force) {
    PVector f = force.get();
    //f.div(mass); // ignoring mass right now
    acceleration.add(f);
  }

  // Turn changes angle
  void turn(float a) {
    heading += a;
  }
  
  // Apply a thrust force
  void thrust() {
    // Offset the angle since we drew the ship vertically
    float angle = heading - PI/2;
    // Polar to cartesian for force vector!
    PVector force = PVector.fromAngle(angle);
    force.mult(0.1);
    applyForce(force); 
    
    force.mult(-2);
    ps.addParticle(location.x,location.y+r,force);
    
    
    // To draw booster
    thrusting = true;
  }

  void wrapEdges() {
    float buffer = r*2;
    if (location.x > width +  buffer) location.x = -buffer;
    else if (location.x <    -buffer) location.x = width+buffer;
    if (location.y > height + buffer) location.y = -buffer;
    else if (location.y <    -buffer) location.y = height+buffer;
  }


  // Draw the ship
  void display() { 
    stroke(0);
    strokeWeight(2);
    pushMatrix();
    translate(location.x,location.y+r);
    rotate(heading);
    fill(175);
    if (thrusting) fill(255,0,0);
    // Booster rockets
    rect(-r/2,r,r/3,r/2);
    rect(r/2,r,r/3,r/2);
    fill(175);
    // A triangle
    beginShape();
    vertex(-r,r);
    vertex(0,-r);
    vertex(r,r);
    endShape(CLOSE);
    rectMode(CENTER);
    popMatrix();
    
    thrusting = false;
  }
}

