// AVOID loading an image within draw(), it is slow
void draw() {
  PImage img = loadImage("tower.jpg");
  image(img, 0, 0);
}