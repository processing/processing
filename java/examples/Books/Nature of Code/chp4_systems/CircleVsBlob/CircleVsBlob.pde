// Smoke Particle System
// Daniel Shiffman <http://www.shiffman.net>

// A basic smoke effect using a particle system
// Each particle is rendered as an alpha masked image


void setup() {
  size(200,200);
  smooth();
  PImage img = loadImage("texture.png");
  background(0);
  image(img,0,0,width,height);
  save("blob.tif");
  
  background(0);
  fill(255);
  noStroke();
  ellipse(100,100,width,height);
  save("circle.tif");
}

void draw() {
  
  
}


