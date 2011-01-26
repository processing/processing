
// Based on code 32-04 (p. 294)


float angle = 0.0;   // Changing angle
float speed = 0.2;  // Speed of growth

void setup() {
  size(200*11, 200*10);
  noStroke();
  smooth();
  fill(0, 60);
}

void draw() {
  background(255);

  for(int y = 0; y < 10; y++) {
    for(int i = 0; i < 11; i++) {
      pushMatrix();
      translate(i*200, y*220);
      circlePhase(0.0);
      circlePhase(QUARTER_PI);
      circlePhase(HALF_PI);
      angle += speed;
      popMatrix();
    }
    
  }
  save("page_366.tif");
  exit();
}

void circlePhase(float phase) {
  float diameter = 65 + (sin(angle + phase) * 130);
  ellipse(100, 100, diameter, diameter);
}
