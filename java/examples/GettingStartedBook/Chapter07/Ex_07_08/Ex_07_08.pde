// Example 07-08 from "Getting Started with Processing" 
// by Reas & Fry. O'Reilly / Make 2010

void setup() {
  size(240, 120);
  smooth();
}

void draw() {
  background(204);
  for (int x = 20; x < width; x += 20) {
    float mx = mouseX / 10;
    float offsetA = random(-mx, mx);
    float offsetB = random(-mx, mx);
    line(x + offsetA, 20, x - offsetB, 100);
  }
}

