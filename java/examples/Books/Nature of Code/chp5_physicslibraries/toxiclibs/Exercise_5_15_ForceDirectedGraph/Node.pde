// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Force directed graph
// Heavily based on: http://code.google.com/p/fidgen/

// Notice how we are using inheritance here!
// We could have just stored a reference to a VerletParticle object
// inside the Node class, but inheritance is a nice alternative

class Node extends VerletParticle2D {

  Node(Vec2D pos) {
    super(pos);
  }

  // All we're doing really is adding a display() function to a VerletParticle
  void display() {
    fill(0,150);
    stroke(0);
    strokeWeight(2);
    ellipse(x,y,16,16);
  }
}

