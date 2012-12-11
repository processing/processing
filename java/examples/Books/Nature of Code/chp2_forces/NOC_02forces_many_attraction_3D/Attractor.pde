// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Attraction

// A class for a draggable attractive body in our world

class Attractor {
  float mass;         // Mass, tied to size
  PVector location;   // Location
  float g;

  Attractor() {
    location = new PVector(0,0);
    mass = 20;
    g = 0.4;
  }


  PVector attract(Mover m) {
    PVector force = PVector.sub(location,m.location);             // Calculate direction of force
    float distance = force.mag();                                 // Distance between objects
    distance = constrain(distance,5.0,25.0);                             // Limiting the distance to eliminate "extreme" results for very close or very far objects
    force.normalize();                                            // Normalize vector (distance doesn't matter here, we just want this vector for direction)
    float strength = (g * mass * m.mass) / (distance * distance); // Calculate gravitional force magnitude
    force.mult(strength);                                         // Get force vector --> magnitude * direction
    return force;
  }

  // Method to display
  void display() {
    stroke(255);
    noFill();
    pushMatrix();
    translate(location.x,location.y,location.z);
    sphere(mass*2);
    popMatrix();
  }
}


