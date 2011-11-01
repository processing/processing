// Example 10-03 from "Getting Started with Processing" 
// by Reas & Fry. O'Reilly / Make 2010

float[] x = new float[3000];

void setup() {
  size(240, 120);
  smooth();
  noStroke();
  fill(255, 200);
  for (int i = 0; i < x.length; i++) {
    x[i] = random(-1000, 200);
  }
}

void draw() {
  background(0);
  for (int i = 0; i < x.length; i++) {
    x[i] += 0.5;
    float y = i * 0.4;
    arc(x[i], y, 12, 12, 0.52, 5.76);
  }
}

