// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Notice how we are using inheritance here!
// We could have just stored a reference to a VerletParticle object
// inside the Particle class, but inheritance is a nice alternative

class Particle extends VerletParticle2D {
  
  float radius = 4;  // Adding a radius for each particle
  
  Particle(float x, float y) {
    super(x,y);
  }

  // All we're doing really is adding a display() function to a VerletParticle
  void display() {
    fill(127);
    stroke(0);
    strokeWeight(2);
    ellipse(x,y,radius*2,radius*2);
  }
}
