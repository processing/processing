// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

ParticleSystem ps;

void setup() {
  size(640,360);
  ps = new ParticleSystem(100,100,5);
}

void draw() {
  background(255);

  ps.display();
  ps.update();
}

void mousePressed() {
  ps.shatter(); 
}
