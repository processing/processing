// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Array of Images for particle textures

ParticleSystem ps;

PImage[] imgs;

void setup() {
  size(640, 360, P2D);

  imgs = new PImage[5];
  imgs[0] = loadImage("corona.png");
  imgs[1] = loadImage("emitter.png");
  imgs[2] = loadImage("particle.png");
  imgs[3] = loadImage("texture.png");
  imgs[4] = loadImage("reflection.png");

  ps = new ParticleSystem(imgs, new PVector(width/2, 50));
}

void draw() {

  // Additive blending!
  blendMode(ADD);

  background(0);
  
  PVector up = new PVector(0,-0.2);
  ps.applyForce(up);
  
  ps.run();
  for (int i = 0; i < 5; i++) {
    ps.addParticle(mouseX,mouseY);
  }
}

