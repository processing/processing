/**
 * Smoke Particle System
 * by Daniel Shiffman.  
 * 
 * A basic smoke effect using a particle system. 
 * Each particle is rendered as an alpha masked image. 
 */

ParticleSystem ps;
Random generator;

void setup() {

  size(640, 200);
  colorMode(RGB, 255, 255, 255, 100);

  // Using a Java random number generator for Gaussian random numbers
  generator = new Random();

  // Create an alpha masked image to be applied as the particle's texture
  PImage msk = loadImage("texture.gif");
  PImage img = new PImage(msk.width,msk.height);
  for (int i = 0; i < img.pixels.length; i++) img.pixels[i] = color(255);
  img.mask(msk);
  ps = new ParticleSystem(0,new Vector3D(width/2,height-20),img);

  smooth();
}

void draw() {
  background(75);

  // Calculate a "wind" force based on mouse horizontal position
  float dx = (mouseX - width/2) / 1000.0;
  Vector3D wind = new Vector3D(dx,0,0);
  wind.display(width/2,50,500);
  ps.add_force(wind);
  ps.run();
  for (int i = 0; i < 2; i++) {
    ps.addParticle();
  }
}










