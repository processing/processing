// Constrains the position of the ellipse to a region
void setup() {
  size(100, 100);
  smooth();
  noStroke();
}

void draw() {
  background(0);
  // Limits mx between 35 and 65
  float mx = constrain(mouseX, 35, 65);
  // Limits my between 40 and 60
  float my = constrain(mouseY, 40, 60);
  fill(102);
  rect(20, 25, 60, 50);
  fill(255);
  ellipse(mx, my, 30, 30);
}