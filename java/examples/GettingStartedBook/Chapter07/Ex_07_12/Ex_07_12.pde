// Example 07-12 from "Getting Started with Processing" 
// by Reas & Fry. O'Reilly / Make 2010

float angle = 0.0;

void draw() {
  float sinval = sin(angle);
  println(sinval);
  float gray = map(sinval, -1, 1, 0, 255);
  background(gray);
  angle += 0.1;
}

