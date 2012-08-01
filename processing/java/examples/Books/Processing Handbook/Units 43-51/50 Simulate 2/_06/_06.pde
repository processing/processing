Particle p;

void setup() {
  size(100, 100);
  noStroke();
  smooth();
  p = new Particle(0, height, 2.2, -4.2, 20.0);
}

void draw() {
  fill(0, 12);
  rect(0, 0, width, height);
  fill(255);
  p.update();
  p.display();
}
