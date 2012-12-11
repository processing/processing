// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Notice how we are using inheritance here!
// We could have just stored a reference to a VerletParticle object
// inside the Particle class, but inheritance is a nice alternative

class Particle extends VerletParticle2D {

  Particle(Vec2D loc) {
    super(loc);
  }

  // All we're doing really is adding a display() function to a VerletParticle
  void display() {
    fill(175);
    stroke(0);
    ellipse(x,y,16,16);
  }
}
