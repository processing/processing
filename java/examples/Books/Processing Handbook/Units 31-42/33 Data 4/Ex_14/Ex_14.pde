int[] y;

void setup() {
  size(100, 100);
  y = new int[width];
}

void draw() {
  background(204);
// Shift the values to the right
  for (int i = y.length - 1; i > 0; i--) {
    y[i] = y[i-1];
  }
// Add new values to the beginning
  y[0] = constrain(mouseY, 0, height - 1);
// Display each pair of values as a line
  for (int i = 1; i < y.length; i++) {
    line(i, y[i], i - 1, y[i-1]);
  }
}
