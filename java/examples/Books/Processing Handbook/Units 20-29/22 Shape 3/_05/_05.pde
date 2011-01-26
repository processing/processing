int x = 40; // X-coordinate
int y = 30; // Y-coordinate
int g = 20; // Gap between eyes
void setup() {
  size(100, 100);
  smooth();
  fill(0);
  noLoop();
}

void draw() {
  background(204);
  face(x, y, g);
}

void face(int x, int y, int gap) {
  line(x, 0, x, y); // Nose Bridge
  line(x, y, x+gap, y); // Nose
  line(x+gap, y, x+gap, 100);
  int mouthY = height - (height-y)/2;
  line(x, mouthY, x+gap, mouthY); // Mouth
  noStroke();
  ellipse(x-gap/2, y/2, 5, 5); // Left eye
  ellipse(x+gap, y/2, 5, 5); // Right eye
}