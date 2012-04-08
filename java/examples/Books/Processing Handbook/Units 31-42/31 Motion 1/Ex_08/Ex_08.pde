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
int direction = 1;

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
  pct += step * direction;
  if ((pct > 1.0) || (pct < 0.0)) {
    direction = direction * -1;
  }
  if (direction == 1) {
    x = beginX + (pct * distX);
    float e = pow(pct, exponent);
    y = beginY + (e * distY);
  } else {
    x = beginX + (pct * distX);
    float e = pow(1.0 - pct, exponent * 2);
    y = beginY + (e * -distY) + distY;
  }
  fill(255);
  ellipse(x, y, 20, 20);
}
