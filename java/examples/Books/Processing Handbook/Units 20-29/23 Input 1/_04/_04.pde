// Multiplying and dividing creates scaling offsets
void setup() {
  size(100, 100);
  smooth();
  noStroke();
}
void draw() {
  background(126);
  ellipse(mouseX, 16, 33, 33); // Top circle
  ellipse(mouseX/2, 50, 33, 33); // Middle circle
  ellipse(mouseX*2, 84, 33, 33); // Bottom circle
}