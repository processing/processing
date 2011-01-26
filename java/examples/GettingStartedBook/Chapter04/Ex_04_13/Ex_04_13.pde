// Example 04-13 from "Getting Started with Processing" 
// by Reas & Fry. O'Reilly / Make 2010

size(480, 120);
background(0);
smooth();
for (int y = 32; y <= height; y += 8) {
  for (int x = 12; x <= width; x += 15) {
    ellipse(x + y, y, 16 - y/10.0, 16 - y/10.0);
  }
}

