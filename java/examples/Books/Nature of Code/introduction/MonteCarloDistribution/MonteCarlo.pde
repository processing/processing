// Daniel Shiffman
// The Nature of Code
// http://www.shiffman.net/

float[] vals;  // Array to count how often a random # is picked
float[] norms; // Normalized version of above

void setup() {
  size(200, 200);
  smooth();
  vals = new float[width];
  norms = new float[width];
}

void draw() {
  background(100);

  // Pick a random number between 0 and 1 based on custom probability function
  float n = montecarlo();

  // What spot in the array did we pick
  int index = int(n*width);
  vals[index]++;
  stroke(255);

  boolean normalization = false;
  float maxy = 0.0;

  // Draw graph based on values in norms array
  // If a value is greater than the height, set normalization to true
  for (int x = 0; x < vals.length; x++) {
    line(x, height, x, height-norms[x]);
    if (vals[x] > height) normalization = true;
    if (vals[x] > maxy) maxy = vals[x];
  }

  // If normalization is true then normalize to height
  // Otherwise, just copy the info
  for (int x = 0; x < vals.length; x++) {
    if (normalization) norms[x] = (vals[x] / maxy) * (height);
    else norms[x] = vals[x];
  }
}

// An algorithm for picking a random number based on monte carlo method
// Here probability is determined by formula y = x
float montecarlo() {
  // Have we found one yet
  boolean foundone = false;
  int hack = 0;  // let's count just so we don't get stuck in an infinite loop by accident
  while (!foundone && hack < 10000) {
    // Pick two random numbers
    float r1 = (float) random(1);
    float r2 = (float) random(1);
    float y = r1*r1;  // y = x*x (change for different results)
    // If r2 is valid, we'll use this one
    if (r2 < y) {
      foundone = true;
      return r1;
    }
    hack++;
  }
  // Hack in case we run into a problem (need to improve this)
  return 0;
}

