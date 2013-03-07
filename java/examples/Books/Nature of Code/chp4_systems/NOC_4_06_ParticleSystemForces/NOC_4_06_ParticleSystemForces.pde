// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

ParticleSystem ps;

void setup() {
  size(640,360);
  ps = new ParticleSystem(new PVector(width/2,50));
}

void draw() {
  background(255);
  
  // Apply gravity force to all Particles
  PVector gravity = new PVector(0,0.1);
  ps.applyForce(gravity);
  
  ps.addParticle();
  ps.run();
}
