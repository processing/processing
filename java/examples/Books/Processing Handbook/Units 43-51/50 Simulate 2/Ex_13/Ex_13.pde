float stiffness = 0.1;
float damping = 0.9;
float velocity = 0.0;
float targetY;
float y;

void setup() {
  size(100, 100);
  noStroke();
}

void draw() {
  fill(0, 12);
  rect(0, 0, width, height);
  fill(255);
  float force = stiffness * (targetY - y); // f = -kx
  velocity = damping * (velocity + force);
  y += velocity;
  rect(10, y, width - 20, 12);
  targetY = mouseY;
}
