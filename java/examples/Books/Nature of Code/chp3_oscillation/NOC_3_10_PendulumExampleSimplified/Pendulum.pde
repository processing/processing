// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Pendulum

// A Simple Pendulum Class
// Includes functionality for user can click and drag the pendulum

class Pendulum  {

  PVector location;    // Location of pendulum ball
  PVector origin;      // Location of arm origin
  float r;             // Length of arm
  float angle;         // Pendulum arm angle
  float aVelocity;     // Angle velocity
  float aAcceleration; // Angle acceleration
  float damping;       // Arbitary damping amount

  // This constructor could be improved to allow a greater variety of pendulums
  Pendulum(PVector origin_, float r_) {
    // Fill all variables
    origin = origin_.get();
    location = new PVector();
    r = r_;
    angle = PI/4;

    aVelocity = 0.0;
    aAcceleration = 0.0;
    damping = 0.995;   // Arbitrary damping
  }

  void go() {
    update();
    display();
  }

  // Function to update location
  void update() {
    float gravity = 0.4;                              // Arbitrary constant
    aAcceleration = (-1 * gravity / r) * sin(angle);  // Calculate acceleration (see: http://www.myphysicslab.com/pendulum1.html)
    aVelocity += aAcceleration;                 // Increment velocity
    aVelocity *= damping;                       // Arbitrary damping
    angle += aVelocity;                         // Increment angle
  }

  void display() {
    location.set(r*sin(angle), r*cos(angle), 0);         // Polar to cartesian conversion
    location.add(origin);                              // Make sure the location is relative to the pendulum's origin

    stroke(0);
    strokeWeight(2);
    // Draw the arm
    line(origin.x, origin.y, location.x, location.y);
    ellipseMode(CENTER);
    fill(175);
    // Draw the ball
    ellipse(location.x, location.y, 48, 48);
  }

}



