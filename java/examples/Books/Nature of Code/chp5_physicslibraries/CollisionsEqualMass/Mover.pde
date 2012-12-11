// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Collisions

class Mover {

  PVector loc;
  PVector vel;
  float bounce = 1.0;
  float r = 20;
  boolean colliding = false;

  Mover(PVector v, PVector l) {
    vel = v.get();
    loc = l.get();
  }

  // Main method to operate object
  void go() {
    update();
    borders();
    display();
  }

  // Method to update location
  void update() {
    loc.add(vel);
  }

  // Check for bouncing off borders
  void borders() {
    if (loc.y > height) {
      vel.y *= -bounce;
      loc.y = height;
    } 
    else if (loc.y < 0) {
      vel.y *= -bounce;
      loc.y = 0;
    } 
    if (loc.x > width) {
      vel.x *= -bounce;
      loc.x = width;
    }  
    else if (loc.x < 0) {
      vel.x *= -bounce;
      loc.x = 0;
    }
  }  

  // Method to display
  void display() {
    ellipseMode(CENTER);
    stroke(0);
    fill(175,200);
    ellipse(loc.x,loc.y,r*2,r*2);
    if (showVectors) {
      drawVector(vel,loc,10);
    }
  }

  void collideEqualMass(Mover other) {
    float d = PVector.dist(loc,other.loc);
    float sumR = r + other.r;
    // Are they colliding?
    if (!colliding && d < sumR) {
      // Yes, make new velocities!
      colliding = true;
      // Direction of one object another
      PVector n = PVector.sub(other.loc,loc);
      n.normalize();

      // Difference of velocities so that we think of one object as stationary
      PVector u = PVector.sub(vel,other.vel);

      // Separate out components -- one in direction of normal
      PVector un = componentVector(u,n);
      // Other component
      u.sub(un);
      // These are the new velocities plus the velocity of the object we consider as stastionary
      vel = PVector.add(u,other.vel);
      other.vel = PVector.add(un,other.vel);
    } 
    else if (d > sumR) {
      colliding = false;
    }
  }
}

PVector componentVector (PVector vector, PVector directionVector) {
  //--! ARGUMENTS: vector, directionVector (2D vectors)
  //--! RETURNS: the component vector of vector in the direction directionVector
  //-- normalize directionVector
  directionVector.normalize();
  directionVector.mult(vector.dot(directionVector));
  return directionVector;
}


