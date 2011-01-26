class Particle {	
  float x, y;    // X-coordinate, y-coordinate
  float vx, vy;  // X velocity, y velocity 
  float radius;  // Particle radius
  float gravity = 0.1;

  Particle(int xpos, int ypos, float velx, float vely, float r) {
    x = xpos;
    y = ypos;
    vx = velx;
    vy = vely;
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
