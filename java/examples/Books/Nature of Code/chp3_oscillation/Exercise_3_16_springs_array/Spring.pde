// Nature of Code 2011
// Daniel Shiffman
// Chapter 3: Oscillation

// Class to describe an anchor point that can connect to "Bob" objects via a spring
// Thank you: http://www.myphysicslab.com/spring2d.html

class Spring { 

  // Location
  PVector anchor;

  // Rest length and spring constant
  float len;
  float k = 0.2;
  
  Bob a;
  Bob b;

  // Constructor
  Spring(Bob a_, Bob b_, int l) {
    a = a_;
    b = b_;
    len = l;
  } 

  // Calculate spring force
  void update() {
    // Vector pointing from anchor to bob location
    PVector force = PVector.sub(a.location, b.location);
    // What is distance
    float d = force.mag();
    // Stretch is difference between current distance and rest length
    float stretch = d - len;

    // Calculate force according to Hooke's Law
    // F = k * stretch
    force.normalize();
    force.mult(-1 * k * stretch);
    a.applyForce(force);
    force.mult(-1);
    b.applyForce(force);
  }


  void display() {
    strokeWeight(2);
    stroke(0);
    line(a.location.x, a.location.y, b.location.x, b.location.y);
  }
}

