// The Particle System

class ParticleSystem {
  // It's just an ArrayList of particle objects
  ArrayList<Particle> particles;

  // The PShape to group all the particle PShapes
  PShape particleShape;

  ParticleSystem(int n) {
    particles = new ArrayList<Particle>();
    // The PShape is a group
    particleShape = createShape(GROUP);

    // Make all the Particles
    for (int i = 0; i < n; i++) {
      Particle p = new Particle();
      particles.add(p);
      // Each particle's PShape gets added to the System PShape
      particleShape.addChild(p.getShape());
    }
  }

  void update() {
    for (Particle p : particles) {
      p.update();
    }
  }

  void setEmitter(float x, float y) {
    for (Particle p : particles) {
      // Each particle gets reborn at the emitter location
      if (p.isDead()) {
        p.rebirth(x, y);
      }
    }
  }

  void display() {
    shape(particleShape);
  }
}

