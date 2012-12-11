// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Box2D Particle System

// A class to describe a group of Particles
// An ArrayList is used to manage the list of Particles 

class ParticleSystem  {

  ArrayList<Particle> particles;    // An ArrayList for all the particles
  PVector origin;         // An origin point for where particles are birthed

  ParticleSystem(int num, PVector v) {
    particles = new ArrayList<Particle>();             // Initialize the ArrayList
    origin = v.get();                        // Store the origin point

      for (int i = 0; i < num; i++) {
      particles.add(new Particle(origin.x,origin.y));    // Add "num" amount of particles to the ArrayList
    }
  }

  void run() {
    // Display all the particles
    for (Particle p: particles) {
      p.display();
    }

    // Particles that leave the screen, we delete them
    // (note they have to be deleted from both the box2d world and our list
    for (int i = particles.size()-1; i >= 0; i--) {
      Particle p = particles.get(i);
      if (p.done()) {
        particles.remove(i);
      }
    }
  }

  void addParticles(int n) {
    for (int i = 0; i < n; i++) {
      particles.add(new Particle(origin.x,origin.y));
    }
  }

  // A method to test if the particle system still has particles
  boolean dead() {
    if (particles.isEmpty()) {
      return true;
    } 
    else {
      return false;
    }
  }

}





