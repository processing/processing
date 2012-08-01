// Move a line while any key is pressed
int x = 20;

void setup() {
  size(100, 100);
  smooth();
  strokeWeight(4);
}

void draw() {
  background(204);
  if (keyPressed == true) { // If the key is pressed
    x++; // add 1 to x
  }
  line(x, 20, x-60, 80);
}