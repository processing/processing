float angle = 0.0; // Changing angle
float speed = 0.05; // Speed of growth

void setup() {
  size(100, 100);
  noStroke();
  smooth();
  fill(255, 180);
}

void draw() {
  background(0);
  circlePhase(0.0);
  circlePhase(QUARTER_PI);
  circlePhase(HALF_PI);
  angle += speed;
}
void circlePhase(float phase) {
  float diameter = 65 + (sin(angle + phase) * 45);
  ellipse(50, 50, diameter, diameter);
}
