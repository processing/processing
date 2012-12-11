// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Particles + Forces

// A very basic Repeller class
class Repeller {
  
  // Gravitational Constant
  float G = 100;
  // Location
  PVector location;
  float r = 10;

  Repeller(float x, float y)  {
    location = new PVector(x,y);
  }

  void display() {
    stroke(0);
    strokeWeight(2);
    fill(175);
    ellipse(location.x,location.y,48,48);
  }

  // Calculate a force to push particle away from repeller
  PVector repel(Particle p) {
    PVector dir = PVector.sub(location,p.location);      // Calculate direction of force
    float d = dir.mag();                       // Distance between objects
    dir.normalize();                           // Normalize vector (distance doesn't matter here, we just want this vector for direction)
    d = constrain(d,5,100);                    // Keep distance within a reasonable range
    float force = -1 * G / (d * d);            // Repelling force is inversely proportional to distance
    dir.mult(force);                           // Get force vector --> magnitude * direction
    return dir;
  }  
}


