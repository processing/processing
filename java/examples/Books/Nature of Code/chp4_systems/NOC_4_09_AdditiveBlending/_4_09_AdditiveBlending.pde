// Smoke Particle System
// Daniel Shiffman <http://www.shiffman.net>

// A basic smoke effect using a particle system
// Each particle is rendered as an alpha masked image

ParticleSystem ps;


PImage img;

void setup() {
  size(800, 200, P2D);

  // Create an alpha masked image to be applied as the particle's texture
  img = loadImage("texture.png");

  ps = new ParticleSystem(0, new PVector(width/2, 50));
  smooth();

}

void draw() {
  
  blendMode(ADD);
  
  background(0);

  ps.run();
  for (int i = 0; i < 10; i++) {
    ps.addParticle();
  }
}

