class Particle {
  
  float x, y; // X-coordinate, y-coordinate
  float vx, vy; // X velocity, y velocity
  float radius; // Particle radius
  float gravity = 0.1;
  
  Particle(int xIn, int yIn, float vxIn, float vyIn, float r) {
    x = xIn;
    y = yIn;
    vx = vxIn;
    vy = vyIn;
    radius = r;
  }
  
  void update() {
    vy = vy + gravity;
    y += vy;
    x += vx;
  }
  
  void display() {
    ellipse(x, y, radius*2, radius*2);
  }
}
