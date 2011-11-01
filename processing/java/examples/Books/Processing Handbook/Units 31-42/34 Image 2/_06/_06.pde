PImage img;
float angle;

void setup() {
  size(100, 100);
  img = loadImage("PT-Shifty-0023.gif");
}

void draw() {
  background(204);
  angle += 0.01;
  translate(50, 50);
  rotate(angle);
  image(img, -100, -100);
}
