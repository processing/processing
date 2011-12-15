/**
 * Multiple Particle Systems
 * by Daniel Shiffman.  
 * 
 * Click the mouse to generate a burst of particles
 * at mouse location. 
 * 
 * Each burst is one instance of a particle system
 * with Particles and CrazyParticles (a subclass of Particle)
 * Note use of Inheritance and Polymorphism here. 
 */

ArrayList<ParticleSystem> systems;

void setup() {
  size(640, 360);
  systems = new ArrayList<ParticleSystem>();
  smooth();
}

void draw() {
  background(0);
  for (ParticleSystem ps: systems) {
    ps.run();
    ps.addParticle();
  }

  if (systems.isEmpty()) {
    fill(255);
    textAlign(CENTER);
    text("click mouse to add particle systems", width/2, height/2);
  }
}

void mousePressed() {
  systems.add(new ParticleSystem(1, new PVector(mouseX, mouseY)));
}










