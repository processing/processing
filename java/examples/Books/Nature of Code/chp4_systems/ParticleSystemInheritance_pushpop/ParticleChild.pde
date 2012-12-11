// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

class ParticleChild extends Particle {

  // We could add variables for only Confetti here if we so

  ParticleChild(PVector l) {
    super(l);
  }

  // Inherits update() from parent

  // Override the display method
  void display() {
    super.display();
    float theta = map(location.x,0,width,0,TWO_PI*2);
    rotate(theta);
    stroke(0);
    line(0,0,50,0);
  }
}

