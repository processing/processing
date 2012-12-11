// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

ParticleSystem ps;

void setup() {
  size(200,200);
  ps = new ParticleSystem(new PVector(width/2,50));
}

void draw() {
  background(255);
  ps.addParticle();
  ps.run();
}
