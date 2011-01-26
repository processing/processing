void setup() {
  size(100, 100);
  noLoop();
  smooth();
}

void draw() {
  stroke(160); // Thick, light gray
  strokeWeight(20);
  line(0, 5, 60, 65);
  line(60, 5, 0, 65);

  stroke(0); // Medium, black
  strokeWeight(10);
  line(30, 20, 90, 80);
  line(90, 20, 30, 80);

  // Draw thin, white X
  stroke(255);
  strokeWeight(2);
  line(20, 38, 80, 98);
  line(80, 38, 20, 98);
}
