
// Based on code 50-12 (p. 486)
// Requires Particle, ArrowParticle classes

int num = 900;
ArrowParticle[] p = new ArrowParticle[num];
float radius = 1.2;

boolean record = false;

void setup() {
  size(513, 666);
  strokeWeight(0.25);
  smooth();
  for (int i = 0; i < p.length; i++) {
    float velX = random(-1, 10);
    float velY = random(-20, -6);
    // Parameters: X, Y, X velocity, Y velocity, Radius
    p[i] = new ArrowParticle(width/4, height, velX, velY, 0.4);
  }
}

void draw() {
  if(record) {
    beginRecord(PDF, "page_476-alt.pdf");
  }
  background(255);
  for (int i = 0; i < p.length; i++) {
    p[i].update();
    p[i].display(); 
  }
  if(record) {
    endRecord();
    record = false; 
  }
}


void mousePressed() {
  record = true;
}


