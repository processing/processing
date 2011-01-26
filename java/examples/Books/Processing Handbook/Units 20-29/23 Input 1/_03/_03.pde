// Add and subtract to create offsets
void setup() {
size(100, 100);
  smooth();
  noStroke();
}
void draw() {
  background(126);
  ellipse(mouseX, 16, 33, 33); // Top circle
  ellipse(mouseX+20, 50, 33, 33); // Middle circle
  ellipse(mouseX-20, 84, 33, 33); // Bottom circle
}