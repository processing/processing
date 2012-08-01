class ArrowParticle extends Particle {
  float angle = 0.0;
  float shaftLength = 20.0;
  ArrowParticle(int ix, int iy, float ivx, float ivy, float ir) {
    super(ix, iy, ivx, ivy, ir);
  }

  void update() {
    super.update();
    angle = atan2(vy, vx);
  }

  void display() {
    stroke(255);
    pushMatrix();
    translate(x, y);
    rotate(angle);
    scale(shaftLength);
    strokeWeight(1.0 / shaftLength);
    line(0, 0, 1, 0);
    line(1, 0, 0.7, -0.3);
    line(1, 0, 0.7, 0.3);
    popMatrix();
  }
}

