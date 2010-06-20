void setup() {
  size(240, 120);
  strokeWeight(12);
  smooth();
}

void draw() {
  background(204);
  stroke(255);
  line(120, 60, mouseX, mouseY); // White line
  stroke(0);
  float mx = mouseX/2 + 60;
  line(120, 60, mx, mouseY); // Black line
}

