// Convert the red values of pixels to line lengths
PImage arch;

void setup() {
  size(100, 100);
  arch = loadImage("arch.jpg");
  arch.loadPixels();
}

void draw() {
  background(204);
  int my = constrain(mouseY, 0, 99);
  for (int i = 0; i < arch.height; i++) {
    color c = arch.pixels[my*width + i]; // Get a pixel
    float r = red(c); // Get the red value
    line(i, 0, i, height / 2 + r / 6);
  }
}
