// Example 05-17 from "Getting Started with Processing" 
// by Reas & Fry. O'Reilly / Make 2010

int x = 80;
int y = 30;
int w = 80;
int h = 60;

void setup() {
  size(240, 120);
}

void draw() {
  background(204);
  if ((mouseX > x) && (mouseX < x+w) &&
    (mouseY > y) && (mouseY < y+h)) {
    fill(0);
  } 
  else {
    fill(255);
  }
  rect(x, y, w, h);
}

