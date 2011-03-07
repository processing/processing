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
  ps = new ParticleSystem(0, new PVector(width/2,height-20 ),img);

  smooth();
}

void draw() {
  background(75);

  // Calculate a "wind" force based on mouse horizontal position
  float dx = (mouseX - width/2) / 1000.0;
  PVector wind = new PVector(dx,0,0);
  displayVector(wind,width/2,50,500);
  ps.add_force(wind);
  ps.run();
  for (int i = 0; i < 2; i++) {
    ps.addParticle();
  }
}

 void displayVector(PVector v, float x, float y, float scayl) {
    pushMatrix();
    float arrowsize = 4;
    // Translate to location to render vector
    translate(x,y);
    stroke(255);
    // Call vector heading function to get direction (note that pointing up is a heading of 0) and rotate
    rotate(v.heading2D());
    // Calculate length of vector & scale it to be bigger or smaller if necessary
    float len = v.mag()*scayl;
    // Draw three lines to make an arrow (draw pointing up since we've rotate to the proper direction)
    line(0,0,len,0);
    line(len,0,len-arrowsize,+arrowsize/2);
    line(len,0,len-arrowsize,-arrowsize/2);
    popMatrix();
  } 










