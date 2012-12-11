// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A soft pendulum (series of connected springs)

class Chain {

  // Chain properties
  float totalLength;  // How long
  int numPoints;      // How many points
  float strength;     // Strength of springs
  float radius;       // Radius of ball at tail
  
  // This list is redundant since we can ask for physics.particles, but in case we have many of these
  // it's a convenient to keep track of our own list
  ArrayList<Particle> particles;

  // Let's keep an extra reference to the tail particle
  // This is just the last particle in the ArrayList
  Particle tail;

  // Some variables for mouse dragging
  PVector offset = new PVector();
  boolean dragged = false;

  // Chain constructor
  Chain(float l, int n, float r, float s) {
    particles = new ArrayList<Particle>();

    totalLength = l;
    numPoints = n;
    radius = r;
    strength = s;

    float len = totalLength / numPoints;

    // Here is the real work, go through and add particles to the chain itself
    for(int i=0; i < numPoints; i++) {
      // Make a new particle with an initial starting location
      Particle particle=new Particle(width/2,i*len);

      // Redundancy, we put the particles both in physics and in our own ArrayList
      physics.addParticle(particle);
      particles.add(particle);

      // Connect the particles with a Spring (except for the head)
      if (i != 0) {
        Particle previous = particles.get(i-1);
        VerletSpring2D spring = new VerletSpring2D(particle,previous,len,strength);
        // Add the spring to the physics world
        physics.addSpring(spring);
      }
    }

    // Keep the top fixed
    Particle head=particles.get(0);
    head.lock();

    // Store reference to the tail
    tail = particles.get(numPoints-1);
    tail.radius = radius;
  }

  // Check if a point is within the ball at the end of the chain
  // If so, set dragged = true;
  void contains(int x, int y) {
    float d = dist(x,y,tail.x,tail.y);
    if (d < radius) {
      offset.x = tail.x - x;
      offset.y = tail.y - y;
      tail.lock();
      dragged = true;
    }
  }

  // Release the ball
  void release() {
    tail.unlock();
    dragged = false;
  }

  // Update tail location if being dragged
  void updateTail(int x, int y) {
    if (dragged) {
      tail.set(x+offset.x,y+offset.y);
    }
  }

  // Draw the chain
  void display() {
    // Draw line connecting all points
    beginShape();
    stroke(0);
    strokeWeight(2);
    noFill();
    for (Particle p : particles) {
      vertex(p.x,p.y);
    }
    endShape();
    tail.display();
  }
}
