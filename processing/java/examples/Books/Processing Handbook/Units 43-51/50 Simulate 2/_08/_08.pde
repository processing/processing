int numParticles = 200;
GenParticle[] p = new GenParticle[numParticles];

void setup() {
  size(100, 100);
  noStroke();
  smooth();
  for (int i = 0; i < p.length; i++) {
    float velX = random(-1, 1);
    float velY = -i;
// Inputs: x, y, x-velocity, y-velocity,
// radius, origin x, origin y
    p[i] = new GenParticle(width / 2, height / 2, velX, velY,
                           5.0, width / 2, height / 2);
  }
}

void draw() {
  fill(0, 36);
  rect(0, 0, width, height);
  fill(255, 60);
  for (int i = 0; i < p.length; i++) {
    p[i].update();
    p[i].regenerate();
    p[i].display();
  }
}
