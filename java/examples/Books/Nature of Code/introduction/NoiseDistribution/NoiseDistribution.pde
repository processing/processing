// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Testing Distribution of Perlin Noise generated #'s vs. Randoms

float[] vals;
float[] norms;
float xoff = 0.0f;

void setup() {
  size(300,200);
  vals = new float[width];
  norms = new float[width];
}

void draw() {
  background(100);
  float n = noise(xoff);
  int index = int(n*width);
  vals[index]++;
  xoff += 0.01;
 stroke(255);
  boolean normalization = false;
  float maxy = 0.0;
  for (int x = 0; x < vals.length; x++) {
    line(x,height,x,height-norms[x]);
    if (vals[x] > height) normalization = true;
    if(vals[x] > maxy) maxy = vals[x];
  }
  for (int x = 0; x < vals.length; x++) {
    if (normalization) norms[x] = (vals[x] / maxy) * (height);
    else norms[x] = vals[x];
  }
}
