class LimitedParticle extends Particle {
  float friction = 0.99;
  LimitedParticle(int ix, int iy, float ivx, float ivy, float ir) {
    super(ix, iy, ivx, ivy, ir);
  }

  void update() {
    vy *= friction;
    vx *= friction;
    super.update();
    limit();
  }

  void limit() {
    if (y > height - radius) {
      vy = -vy;
      y = constrain(y, -height * height, height - radius);
    }
    if ((x < radius) || (x > width - radius)) {
      vx = -vx;
      x = constrain(x, radius, width - radius);
    }
  }
}
