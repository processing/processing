void setup() {
  size(100, 100);
}

void draw() {
// Constrain to not exceed the boundary of the array
  int mx = constrain(mouseX, 0, 99);
  int my = constrain(mouseY, 0, 99);
  loadPixels();
  pixels[my*width + mx] = color(0);
  updatePixels();
}
