void setup() {
  size(100, 100);
}

void draw() {
  float gray = map(second(), 0, 59, 0, 255);
  color c = color(gray);
  int index = frameCount % (width * height);
  loadPixels();
  pixels[index] = c;
  updatePixels();
}
