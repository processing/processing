PImage img;

void setup() {
  size(480, 120);
  img = loadImage("lunar.jpg");
}

void draw() {
  background(0);
  image(img, 0, 0, mouseX * 2, mouseY * 2);
}

