float angle = 0.0; // Current angle
float speed = 0.05; // Speed of motion
float radius = 30.0; // Range of motion
float sx = 2.0;
float sy = 2.0;

void setup() {
  size(100, 100);
  noStroke();
  smooth();
}

void draw() {
  fill(0, 4);
  rect(0, 0, width, height);
  angle += speed; // Update the angle
  float sinval = sin(angle);
  float cosval = cos(angle);
  
  // Set the position of the small circle based on new
  // values from sine and cosine
  float x = 50 + (cosval * radius);
  float y = 50 + (sinval * radius);
  fill(255);
  ellipse(x, y, 2, 2); // Draw smaller circle

  // Set the position of the large circles based on the
  // new position of the small circle
  float x2 = x + cos(angle * sx) * radius / 2;
  float y2 = y + sin(angle * sy) * radius / 2;
  ellipse(x2, y2, 6, 6); // Draw larger circle
}
