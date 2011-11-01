void setup() {
  size(100, 100);
  noStroke();
  smooth();
  noLoop();
}

void draw() {
  fill(255);
  ellipse(50, 50, 60, 60); // White circle
  fill(0);
  ellipse(50+10, 50, 30, 30); // Black circle
  fill(255);
  ellipse(50+16, 45, 6, 6); // Small, white circle
}