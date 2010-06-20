PImage img1;
PImage img2;

void setup() {
  size(480, 120);
  img1 = loadImage("lunar.jpg");
  img2 = loadImage("capsule.jpg");
}

void draw() {
  image(img1, -120, 0);
  image(img1, 130, 0, 240, 120);
  image(img2, 300, 0, 240, 120);
}

