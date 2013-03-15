// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

ArrayList<Particle> particles;

void setup() {
  size(640,360);
  particles = new ArrayList<Particle>();
}

void draw() {
  background(255);

  particles.add(new Particle(new PVector(width/2,50)));
  
  // Looping through backwards to delete
  for (int i = particles.size()-1; i >= 0; i--) {
    Particle p = particles.get(i);
    p.run();
    if (p.isDead()) {
      particles.remove(i);
    }
  }
}




