ParticleSystem ps;

void setup() {
  size(800,200);
  smooth();
  ps = new ParticleSystem(new PVector(width/2,50));
}

void draw() {
  background(255);
  ps.addParticle();
  ps.run();
}
