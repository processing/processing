int numLines = 12;
float[] x = new float[numLines];
float[] speed = new float[numLines];
float offset = 8; // Set space between lines

void setup() {
  size(100, 100);
  smooth();
  strokeWeight(10);
  for (int i = 0; i < numLines; i++) {
    x[i] = i; // Set initial position
    speed[i] = 0.1 + (i / offset); // Set initial speed
  }
}

void draw() {
  background(204);
  for (int i = 0; i < x.length; i++) {
    x[i] += speed[i]; // Update line position
    if (x[i] > (width + offset)) { // If off the right,
      x[i] = -offset * 2; // return to the left
    }
    float y = i * offset; // Set y-coordinate for line
    line(x[i], y, x[i] + offset, y + offset); // Draw line
  }
}
