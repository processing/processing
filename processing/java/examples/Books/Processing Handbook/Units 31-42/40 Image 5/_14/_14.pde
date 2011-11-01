// Convert pixel values into a circle's diameter
PImage arch;
int index;

void setup() {
  size(100, 100);
  smooth();
  fill(0);
  arch = loadImage("arch.jpg");
  arch.loadPixels();
}

void draw() {
  background(204);
  color c = arch.pixels[index]; // Get a pixel
  float r = red(c) / 3.0; // Get the red value
  ellipse(width / 2, height / 2, r, r);
  index++;
  if (index == width*height) {
    index = 0; // Return to the first pixel
  }
}
