float x = 0.0; // X-coordinate
float y = 50.0; // Y-coordinate
float angle = 0.0; // Direction of motion
float speed = 0.5; // Speed of motion

void setup() {
  size(100, 100);
  background(0);
  stroke(255, 130);
  randomSeed(121); // Force the same random values
}

void draw() {
  angle += random(-0.3, 0.3);
  x += cos(angle) * speed; // Update x-coordinate
  y += sin(angle) * speed; // Update y-coordinate
  translate(x, y);
  rotate(angle);
  line(0, -10, 0, 10);
}
