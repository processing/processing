// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A class for a draggable attractive body in our world

class Attractor {
  float mass;    // Mass, tied to size
  PVector location;   // Location
  boolean dragging = false; // Is the object being dragged?
  boolean rollover = false; // Is the mouse over the ellipse?
  PVector drag;  // holds the offset for when object is clicked on

  Attractor() {
    location = new PVector(width/2,height/2);
    mass = 10;
    drag = new PVector(0.0,0.0);
  }

  PVector attract(Mover m) {
    PVector force = PVector.sub(location,m.location);   // Calculate direction of force
    float d = force.mag();                              // Distance between objects
    d = constrain(d,5.0,25.0);                        // Limiting the distance to eliminate "extreme" results for very close or very far objects
    force.normalize();                                  // Normalize vector (distance doesn't matter here, we just want this vector for direction)
    float strength = (g * mass * m.mass) / (d * d);      // Calculate gravitional force magnitude
    force.mult(strength);                                  // Get force vector --> magnitude * direction
    return force;
  }

  // Method to display
  void display() {
    ellipseMode(CENTER);
    stroke(0);
    if (dragging) fill (50);
    else if (rollover) fill(100);
    else fill(0);
    ellipse(location.x,location.y,mass*6,mass*6);
  }

  // The methods below are for mouse interaction
  void clicked(int mx, int my) {
    float d = dist(mx,my,location.x,location.y);
    if (d < mass) {
      dragging = true;
      drag.x = location.x-mx;
      drag.y = location.y-my;
    }
  }

  void rollover(int mx, int my) {
    float d = dist(mx,my,location.x,location.y);
    if (d < mass) {
      rollover = true;
    } 
    else {
      rollover = false;
    }
  }

  void stopDragging() {
    dragging = false;
  }



  void drag() {
    if (dragging) {
      location.x = mouseX + drag.x;
      location.y = mouseY + drag.y;
    }
  }

}


