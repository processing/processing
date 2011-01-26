// Convert the blue values from one row of the image
// to the coordinates for a series of lines
PImage arch;

void setup() {
  size(100, 100);
  smooth();
  arch = loadImage("arch.jpg");
  arch.loadPixels();
}

void draw() {
  background(204);
  int mx = constrain(mouseX, 0, arch.width - 1);
  int offset = mx * arch.width;
  beginShape(LINES);
  for (int i = 0; i < arch.width; i += 2) {
    float r1 = blue(arch.pixels[offset + i]);
    float r2 = blue(arch.pixels[offset + i + 1]);
    float vx = map(r1, 0, 255, 0, height);
    float vy = map(r2, 0, 255, 0, height);
    vertex(vx, vy);
  }
  endShape();
}
