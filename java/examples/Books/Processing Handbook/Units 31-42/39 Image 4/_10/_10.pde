PImage img1, img2;

void setup() {
  size(100, 100);
  img1 = loadImage("forest.jpg");
  img2 = loadImage("airport.jpg");
}

void draw() {
  background(255);
  image(img1, 0, 0);
  int my = constrain(mouseY, 0, 67);
  copy(img2, 0, my, 100, 33, 0, my, 100, 33);
}
