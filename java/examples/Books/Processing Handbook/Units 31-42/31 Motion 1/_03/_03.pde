float x = 50.0; // X-coordinate
float y = 50.0; // Y-coordinate
float radius = 15.0; // Radius of the circle
float speedX = 1.0; // Speed of motion on the x-axis
float speedY = 0.4; // Speed of motion on the y-axis
int directionX = 1; // Direction of motion on the x-axis
int directionY = -1; // Direction of motion on the y-axis

void setup() {
  size(100, 100);
  smooth();
  noStroke();
  ellipseMode(RADIUS);
}

void draw() {
  fill(0, 12);
  rect(0, 0, width, height);
  fill(255);
  ellipse(x, y, radius, radius);
  x += speedX * directionX;
  if ((x > width-radius) || (x < radius)) {
    directionX = -directionX; // Change direction
  }
  y += speedY * directionY;

  if ((y > height-radius) || (y < radius)) {
    directionY = -directionY; // Change direction
  }
}