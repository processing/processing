float x1 = -20;
float x2 = 20;

void setup() {
  size(240, 120);
  smooth();
  noStroke(); 
}

void draw() {
  background(0);
  x1 += 0.5;
  x2 += 0.5;
  arc(x1, 30, 40, 40, 0.52, 5.76);
  arc(x2, 90, 40, 40, 0.52, 5.76);
}

