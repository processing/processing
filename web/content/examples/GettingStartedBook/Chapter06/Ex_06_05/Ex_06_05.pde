PImage img;

void setup() {
  size(480, 120);
  img = loadImage("clouds.png");
}

void draw() {
  background(204);
  image(img, 0, 0);
  image(img, 0, mouseY * -1);
}

