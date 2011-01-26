// Example 07-06 from "Getting Started with Processing" 
// by Reas & Fry. O'Reilly / Make 2010

int startX = 20;     // Initial x-coordinate
int stopX = 160;     // Final x-coordinate
int startY = 30;     // Initial y-coordinate
int stopY = 80;      // Final y-coordinate
float x = startX;    // Current x-coordinate
float y = startY;    // Current y-coordinate
float step = 0.005;  // Size of each step (0.0 to 1.0)
float pct = 0.0;     // Percentage traveled (0.0 to 1.0)

void setup() {
  size(240, 120);
  smooth();
}

void draw() {
  background(0);
  if (pct < 1.0) {
    x = startX + ((stopX-startX) * pct);
    y = startY + ((stopY-startY) * pct);
    pct += step;
  }
  ellipse(x, y, 20, 20);
}

