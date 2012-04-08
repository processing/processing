void setup() {
  size(100, 100);
}

void draw() {
  background(204);
  // Assigns the horizontal value of the cursor to x
  float x = mouseX;
  // Assigns the vertical value of the cursor to y
  float y = mouseY;
  line(x, y, x+20, y-40);
  line(x+10, y, x+30, y-40);
  line(x+20, y, x+40, y-40);
}
