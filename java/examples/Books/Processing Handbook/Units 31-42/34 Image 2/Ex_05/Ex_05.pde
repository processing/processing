PImage img;
float x;

void setup() {
  size(100, 100);
  img = loadImage("PT-Shifty-0020.gif");
}

void draw() {
  background(204);
  x += 0.5;
  if (x > width) {
    x = -width;
  }
  image(img, x, 0);
}
