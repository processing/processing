// Smoke Particle Syste
// Daniel Shiffman <http://www.shiffman.net>

// A class to describe a group of Particles
// An ArrayList is used to manage the list of Particles 

class ParticleSystem {

  ArrayList<Particle> particles;    // An arraylist for all the particles
  PVector origin;        // An origin point for where particles are birthed
  PImage img;
  
  ParticleSystem(int num, PVector v, PImage img_) {
    particles = new ArrayList<Particle>();              // Initialize the arraylist
    origin = v.get();                        // Store the origin point
    img = img_;
    for (int i = 0; i < num; i++) {
      particles.add(new Particle(origin, img));    // Add "num" amount of particles to the arraylist
    }
  }

  void run() {
    Iterator<Particle> it = particles.iterator();
    while (it.hasNext()) {
      Particle p = it.next();
      p.run();
      if (p.dead()) {
        it.remove();
      }
    }
  }
  
  // Method to add a force vector to all particles currently in the system
  void applyForce(PVector dir) {
    // Enhanced loop!!!
    for (Particle p: particles) {
      p.applyForce(dir);
    }
  
  }  

  void addParticle() {
    particles.add(new Particle(origin,img));
  }

  void addParticle(Particle p) {
    particles.add(p);
  }

  // A method to test if the particle system still has particles
  boolean dead() {
    if (particles.isEmpty()) {
      return true;
    } else {
      return false;
    }
  }

}



