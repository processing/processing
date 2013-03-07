// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A class to describe a group of Particles
// An ArrayList is used to manage the list of Particles 

class ParticleSystem {

  ArrayList<Particle> particles;    // An arraylist for all the particles

  PImage[] textures;

  ParticleSystem(PImage[] imgs, PVector v) {
    textures = imgs;
    particles = new ArrayList();              // Initialize the arraylist
  }

  void run() {
    for (int i = particles.size()-1; i >= 0; i--) {
      Particle p = particles.get(i);
      p.run();
      if (p.isDead()) {
        particles.remove(i);
      }
    }
  }

  void addParticle(float x, float y) {
    int r = int(random(textures.length));
    particles.add(new Particle(x,y,textures[r]));
  }
  
  
  void applyForce(PVector f) {
    for (Particle p : particles) {
      p.applyForce(f);
    }
  }

  void addParticle(Particle p) {
    particles.add(p);
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

