int num = 50;
int[] x = new int[num];
int[] y = new int[num];
int indexPosition = 0;

void setup() {
  size(100, 100);
  noStroke();
  smooth();
  fill(255, 102);
}

void draw() {
  background(0);
  x[indexPosition] = mouseX;
  y[indexPosition] = mouseY;
// Cycle between 0 and the number of elements
  indexPosition = (indexPosition + 1) % num;
  for (int i = 0; i < num; i++) {
// Set the array position to read
    int pos = (indexPosition + i) % num;
    float radius = (num - i) / 2.0;
    ellipse(x[pos], y[pos], radius, radius);
  }
}
