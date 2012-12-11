// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

Particle p;

void setup() {
  size(800, 200);
  p = new Particle(new PVector(width/2, 20));
  background(255);
  smooth();
}

void draw() {
  if (mousePressed) {
  noStroke();
  fill(255, 5);
  rect(0, 0, width, height);

  p.run();
  if (p.isDead()) {
    println("Particle dead!");
  }
  }
}


