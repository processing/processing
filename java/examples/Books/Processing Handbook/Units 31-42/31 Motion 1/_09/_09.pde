float beginX = 20.0; // Initial x-coordinate
float beginY = 10.0; // Initial y-coordinate
float endX = 70.0; // Final x-coordinate
float endY = 80.0; // Final y-coordinate
float distX; // X-axis distance to move
float distY; // Y-axis distance to move
float exponent = 3.0; // Determines the curve
float x = 0.0; // Current x-coordinate
float y = 0.0; // Current y-coordinate
float step = 0.01; // Size of each step along the path
float pct = 0.0; // Percentage traveled (0.0 to 1.0)

void setup() {
  size(100, 100);
  noStroke();
  smooth();
  distX = endX - beginX;
  distY = endY - beginY;
}

void draw() {
  fill(0, 2);
  rect(0, 0, width, height);
  if (pct < 1.0) {
    pct = pct + step;
    float rate = pow(pct, exponent);
    x = beginX + (rate * distX);
    y = beginY + (rate * distY);
  }
  fill(255);
  ellipse(x, y, 20, 20);
}
void mousePressed() {
  pct = 0.0;
  beginX = x;
  beginY = y;
  distX = mouseX - x;
  distY = mouseY - y;
}
