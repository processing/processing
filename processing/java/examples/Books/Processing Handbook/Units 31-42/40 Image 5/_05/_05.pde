PImage arch;

void setup() {
  size(100, 100);
  noStroke();
  arch = loadImage("arch.jpg");
}

void draw() {
  background(arch);
// Constrain to not exceed the boundary of the array
  int mx = constrain(mouseX, 0, 99);
  int my = constrain(mouseY, 0, 99);
  loadPixels();
  color c = pixels[my*width + mx];
  fill(c);
  rect(20, 20, 60, 60);
}
