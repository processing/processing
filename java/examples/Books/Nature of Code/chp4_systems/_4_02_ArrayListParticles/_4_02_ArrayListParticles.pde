ArrayList<Particle> particles;

void setup() {
  size(800,200);
  particles = new ArrayList<Particle>();
  smooth();
}

void draw() {
  background(255);

  particles.add(new Particle(new PVector(width/2,50)));
  
  // Using the iterator 
  Iterator<Particle> it = particles.iterator();
  while (it.hasNext()) {
    Particle p = it.next();
    p.run();
    if (p.isDead()) {
      it.remove();
    }
  }
}




