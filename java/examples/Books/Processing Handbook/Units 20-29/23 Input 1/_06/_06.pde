// Exponential functions can create nonlinear relations
// between the mouse and shapes affected by the mouse
void setup() {
  size(100, 100);
  smooth();
  noStroke();
}

void draw() {
  background(126);
  float normX = mouseX / float(width);
  ellipse(mouseX, 16, 33, 33); // Top
  ellipse(pow(normX, 4) * width, 50, 33, 33); // Middle
  ellipse(pow(normX, 8) * width, 84, 33, 33); // Bottom
}