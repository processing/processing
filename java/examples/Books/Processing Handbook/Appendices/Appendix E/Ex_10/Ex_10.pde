int res = 16; // Number of data elements
float[] x = new float[res]; // Create x-coordinate array
float[] y = new float[res]; // Create y-coordinate array

void setup() {
  size(100, 100);
  for (int i = 0; i < res; i++) {
    x[i] = cos(PI/res * i); // Sets x-coordinates
    y[i] = sin(PI/res * i); // Sets y-coordinates
  }
}

void draw() {
  for (int i = 0; i < res; i++) { // Access each point
    point(50 + x[i]*40, 50 + y[i]*40); // Draws point on a curve
  }
}